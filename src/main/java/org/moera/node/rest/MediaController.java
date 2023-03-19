package org.moera.node.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.AuthCategory;
import org.moera.node.auth.AuthenticationCategory;
import org.moera.node.auth.AuthenticationException;
import org.moera.node.auth.UserBlockedException;
import org.moera.node.config.Config;
import org.moera.node.data.BlockedOperation;
import org.moera.node.data.Comment;
import org.moera.node.data.Entry;
import org.moera.node.data.EntryRepository;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.MediaFileOwnerRepository;
import org.moera.node.data.MediaFileRepository;
import org.moera.node.data.Posting;
import org.moera.node.global.ApiController;
import org.moera.node.global.RequestContext;
import org.moera.node.media.InvalidImageException;
import org.moera.node.media.MediaOperations;
import org.moera.node.media.MediaPathNotSetException;
import org.moera.node.media.ThresholdReachedException;
import org.moera.node.model.CommentInfo;
import org.moera.node.model.EntryInfo;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.PostingFeatures;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.PrivateMediaFileInfo;
import org.moera.node.model.PublicMediaFileInfo;
import org.moera.node.model.ValidationFailure;
import org.moera.node.operations.BlockedUserOperations;
import org.moera.node.operations.FeedOperations;
import org.moera.node.operations.PostingOperations;
import org.moera.node.util.DigestingOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@ApiController
@RequestMapping("/moera/api/media")
public class MediaController {

    private static final Logger log = LoggerFactory.getLogger(MediaController.class);

    @Inject
    private Config config;

    @Inject
    private RequestContext requestContext;

    @Inject
    private MediaFileRepository mediaFileRepository;

    @Inject
    private MediaFileOwnerRepository mediaFileOwnerRepository;

    @Inject
    private EntryRepository entryRepository;

    @Inject
    private MediaOperations mediaOperations;

    @Inject
    private PostingOperations postingOperations;

    @Inject
    private BlockedUserOperations blockedUserOperations;

    @Inject
    private FeedOperations feedOperations;

    @Inject
    @PersistenceContext
    private EntityManager entityManager;

    @PostConstruct
    public void init() throws Exception {
        if (ObjectUtils.isEmpty(config.getMedia().getPath())) {
            throw new MediaPathNotSetException("Path not set");
        }
        try {
            Path path = FileSystems.getDefault().getPath(config.getMedia().getPath());
            if (!Files.exists(path)) {
                throw new MediaPathNotSetException("Not found");
            }
            if (!Files.isDirectory(path)) {
                throw new MediaPathNotSetException("Not a directory");
            }
            if (!Files.isWritable(path)) {
                throw new MediaPathNotSetException("Not writable");
            }
            path = path.resolve(MediaOperations.TMP_DIR);
            if (!Files.exists(path)) {
                try {
                    Files.createDirectory(path);
                } catch (FileAlreadyExistsException e) {
                    // ok
                } catch (Exception e) {
                    throw new MediaPathNotSetException("Cannot create tmp/ subdirectory: " + e.getMessage());
                }
            }
        } catch (InvalidPathException e) {
            throw new MediaPathNotSetException("Path is invalid");
        }
    }

    private String toContentType(MediaType mediaType) {
        return mediaType.getType() + "/" + mediaType.getSubtype();
    }

    private DigestingOutputStream transfer(InputStream in, OutputStream out, Long contentLength) throws IOException {
        int maxSize = new PostingFeatures(requestContext.getOptions(), requestContext).getMediaMaxSize();
        return MediaOperations.transfer(in, out, contentLength, maxSize);
    }

    @PostMapping("/public")
    @Transactional
    public PublicMediaFileInfo postPublic(@RequestHeader("Content-Type") MediaType mediaType,
                                          @RequestHeader(value = "Content-Length", required = false) Long contentLength,
                                          InputStream in) throws IOException {
        log.info("POST /media/public (Content-Type: {}, Content-Length: {})",
                LogUtil.format(mediaType.toString()), LogUtil.format(contentLength));

        if (requestContext.getClientName() == null) {
            throw new AuthenticationException();
        }
        if (isBlocked()) {
            throw new UserBlockedException();
        }

        var tmp = mediaOperations.tmpFile();
        try {
            DigestingOutputStream out = transfer(in, tmp.getOutputStream(), contentLength);
            MediaFile mediaFile = mediaOperations.putInPlace(
                    out.getHash(), toContentType(mediaType), tmp.getPath(), out.getDigest());
            mediaFile.setExposed(true);
            mediaFile = mediaFileRepository.save(mediaFile);

            return new PublicMediaFileInfo(mediaFile);
        } catch (InvalidImageException e) {
            throw new ValidationFailure("media.image-invalid");
        } catch (ThresholdReachedException e) {
            throw new ValidationFailure("media.wrong-size");
        } catch (IOException e) {
            throw new OperationFailure("media.storage-error");
        } finally {
            Files.deleteIfExists(tmp.getPath());
        }
    }

    private boolean isBlocked() {
        return (!requestContext.getOptions().getBool("posting.non-admin.allowed")
                    || blockedUserOperations.isBlocked(BlockedOperation.POSTING))
                && blockedUserOperations.isBlocked(BlockedOperation.COMMENT);
    }

    @PostMapping("/private")
    @Transactional
    public PrivateMediaFileInfo postPrivate(@RequestHeader("Content-Type") MediaType mediaType,
                                            @RequestHeader(value = "Content-Length", required = false) Long contentLength,
                                            InputStream in) throws IOException {
        log.info("POST /media/private (Content-Type: {}, Content-Length: {})",
                LogUtil.format(mediaType.toString()), LogUtil.format(contentLength));

        if (requestContext.getClientName() == null) {
            throw new AuthenticationException();
        }
        if (isBlocked()) {
            throw new UserBlockedException();
        }

        var tmp = mediaOperations.tmpFile();
        try {
            DigestingOutputStream out = transfer(in, tmp.getOutputStream(), contentLength);
            String id = out.getHash();
            byte[] digest = out.getDigest();

            MediaFile mediaFile = mediaOperations.putInPlace(id, toContentType(mediaType), tmp.getPath(), digest);
            mediaFile = entityManager.merge(mediaFile); // entity is detached after putInPlace() transaction closed
            MediaFileOwner mediaFileOwner = mediaOperations.own(mediaFile,
                    requestContext.isAdmin() ? null : requestContext.getClientName());
            mediaFileOwner.addPosting(postingOperations.newPosting(mediaFileOwner));

            return new PrivateMediaFileInfo(mediaFileOwner, null);
        } catch (InvalidImageException e) {
            throw new ValidationFailure("media.image-invalid");
        } catch (ThresholdReachedException e) {
            throw new ValidationFailure("media.wrong-size");
        } catch (IOException e) {
            throw new OperationFailure("media.storage-error");
        } finally {
            Files.deleteIfExists(tmp.getPath());
        }
    }

    private MediaFile getMediaFile(String id) {
        MediaFile mediaFile = mediaFileRepository.findById(id)
                .orElseThrow(() -> new ObjectNotFoundFailure("media.not-found"));
        if (mediaFile.isExposed()) {
            return mediaFile;
        } else {
            throw new ObjectNotFoundFailure("media.not-found");
        }
    }

    private MediaFileOwner getMediaFileOwner(UUID id) {
        MediaFileOwner mediaFileOwner = mediaFileOwnerRepository.findFullById(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("media.not-found"));
        if (!requestContext.isPrincipal(mediaFileOwner.getViewE(requestContext.nodeName()))) {
            throw new ObjectNotFoundFailure("media.not-found");
        }
        return mediaFileOwner;
    }

    @GetMapping("/public/{id}/info")
    @Transactional
    public PublicMediaFileInfo getInfoPublic(@PathVariable String id) {
        log.info("GET /media/public/{id}/info (id = {})", LogUtil.format(id));

        return new PublicMediaFileInfo(getMediaFile(id));
    }

    @GetMapping("/private/{id}/info")
    @AuthenticationCategory(AuthCategory.VIEW_MEDIA)
    @Transactional
    public PrivateMediaFileInfo getInfoPrivate(@PathVariable UUID id) {
        log.info("GET /media/private/{id}/info (id = {})", LogUtil.format(id));

        return new PrivateMediaFileInfo(getMediaFileOwner(id), null);
    }

    @GetMapping("/public/{id}/data")
    @Transactional
    public ResponseEntity<Resource> getDataPublic(@PathVariable String id,
                                                  @RequestParam(required = false) Integer width,
                                                  @RequestParam(required = false) Boolean download) {
        log.info("GET /media/public/{id}/data (id = {})", LogUtil.format(id));

        return mediaOperations.serve(getMediaFile(id), width, download);
    }

    @GetMapping("/private/{id}/data")
    @AuthenticationCategory(AuthCategory.VIEW_MEDIA)
    @Transactional
    public ResponseEntity<Resource> getDataPrivate(@PathVariable UUID id,
                                                   @RequestParam(required = false) Integer width,
                                                   @RequestParam(required = false) Boolean download) {
        log.info("GET /media/private/{id}/data (id = {})", LogUtil.format(id));

        return mediaOperations.serve(getMediaFileOwner(id).getMediaFile(), width, download);
    }

    @GetMapping("/private/{id}/parent")
    @Transactional
    public List<EntryInfo> getParentPrivate(@PathVariable UUID id) {
        log.info("GET /media/private/{id}/parent (id = {})", LogUtil.format(id));

        MediaFileOwner mediaFileOwner = getMediaFileOwner(id);
        Set<Entry> entries = entryRepository.findByMediaId(mediaFileOwner.getId());
        List<EntryInfo> parents = new ArrayList<>();
        for (Entry entry : entries) {
            if (entry instanceof Posting) {
                PostingInfo postingInfo = new PostingInfo((Posting) entry, requestContext);
                feedOperations.fillFeedSheriffs(postingInfo);
                parents.add(new EntryInfo(postingInfo));
            }
            if (entry instanceof Comment) {
                parents.add(new EntryInfo(new CommentInfo((Comment) entry, requestContext)));
            }
        }

        return parents;
    }

}
