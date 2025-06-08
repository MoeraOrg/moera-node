package org.moera.node.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import org.moera.lib.node.types.BlockedOperation;
import org.moera.lib.node.types.EntryInfo;
import org.moera.lib.node.types.PrivateMediaFileInfo;
import org.moera.lib.node.types.PublicMediaFileInfo;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.node.types.validate.ValidationFailure;
import org.moera.lib.util.LogUtil;
import org.moera.node.auth.AuthenticationException;
import org.moera.node.auth.UserBlockedException;
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
import org.moera.node.media.ThresholdReachedException;
import org.moera.node.model.CommentInfoUtil;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.PostingFeaturesUtil;
import org.moera.node.model.PostingInfoUtil;
import org.moera.node.model.PrivateMediaFileInfoUtil;
import org.moera.node.model.PublicMediaFileInfoUtil;
import org.moera.node.ocrspace.OcrSpace;
import org.moera.node.operations.BlockedUserOperations;
import org.moera.node.operations.EntryOperations;
import org.moera.node.operations.FeedOperations;
import org.moera.node.operations.PostingOperations;
import org.moera.node.util.DigestingOutputStream;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    private EntryOperations entryOperations;

    @Inject
    private BlockedUserOperations blockedUserOperations;

    @Inject
    private FeedOperations feedOperations;

    @Inject
    @PersistenceContext
    private EntityManager entityManager;

    private String toContentType(MediaType mediaType) {
        return mediaType.getType() + "/" + mediaType.getSubtype();
    }

    private DigestingOutputStream transfer(InputStream in, OutputStream out, Long contentLength) throws IOException {
        int maxSize = PostingFeaturesUtil.build(requestContext.getOptions(), requestContext).getMediaMaxSize();
        return MediaOperations.transfer(in, out, contentLength, maxSize);
    }

    @PostMapping("/public")
    @Transactional
    public PublicMediaFileInfo postPublic(
        @RequestHeader("Content-Type") MediaType mediaType,
        @RequestHeader(value = "Content-Length", required = false) Long contentLength,
        InputStream in
    ) throws IOException {
        log.info(
            "POST /media/public (Content-Type: {}, Content-Length: {})",
            LogUtil.format(mediaType.toString()), LogUtil.format(contentLength)
        );

        if (
            !requestContext.isAdmin(Scope.UPLOAD_PUBLIC_MEDIA)
            && requestContext.getClientName(Scope.UPLOAD_PUBLIC_MEDIA) == null
        ) {
            throw new AuthenticationException();
        }
        if (isBlocked()) {
            throw new UserBlockedException();
        }

        var tmp = mediaOperations.tmpFile();
        try {
            DigestingOutputStream out = transfer(in, tmp.outputStream(), contentLength);
            MediaFile mediaFile = mediaOperations.putInPlace(
                out.getHash(), toContentType(mediaType), tmp.path(), out.getDigest(), true
            );
            mediaFile = mediaFileRepository.save(mediaFile);

            return PublicMediaFileInfoUtil.build(mediaFile);
        } catch (InvalidImageException e) {
            throw new ValidationFailure("media.image-invalid");
        } catch (ThresholdReachedException e) {
            throw new ValidationFailure("media.wrong-size");
        } catch (IOException e) {
            throw new OperationFailure("media.storage-error");
        } finally {
            Files.deleteIfExists(tmp.path());
        }
    }

    private boolean isBlocked() {
        return (
            !requestContext.getOptions().getBool("posting.non-admin.allowed")
            || blockedUserOperations.isBlocked(BlockedOperation.POSTING)
        )
            && blockedUserOperations.isBlocked(BlockedOperation.COMMENT);
    }

    @PostMapping({"/private", "/private/{clientName}"})
    @Transactional
    public PrivateMediaFileInfo postPrivate(
        @RequestHeader("Content-Type") MediaType mediaType,
        @RequestHeader(value = "Content-Length", required = false) Long contentLength,
        @PathVariable String clientName,
        InputStream in
    ) throws IOException {
        log.info(
            "POST /media/private (Content-Type: {}, Content-Length: {})",
            LogUtil.format(mediaType.toString()), LogUtil.format(contentLength)
        );

        if (Objects.equals(clientName, requestContext.nodeName())) {
            clientName = null;
        }

        boolean mediaUploadScope = clientName == null
            ? requestContext.isAdmin(Scope.UPLOAD_PRIVATE_MEDIA)
            : requestContext.isClient(clientName, Scope.UPLOAD_PRIVATE_MEDIA);
        if (!mediaUploadScope) {
            throw new AuthenticationException();
        }
        if (clientName != null && isBlocked()) {
            throw new UserBlockedException();
        }

        var tmp = mediaOperations.tmpFile();
        try {
            DigestingOutputStream out = transfer(in, tmp.outputStream(), contentLength);
            String id = out.getHash();
            byte[] digest = out.getDigest();

            MediaFile mediaFile = mediaOperations.putInPlace(
                id, toContentType(mediaType), tmp.path(), digest, false
            );
            // the entity is detached after putInPlace() transaction closed
            mediaFile = entityManager.merge(mediaFile);
            MediaFileOwner mediaFileOwner = mediaOperations.own(mediaFile, clientName);
            mediaFileOwner.addPosting(postingOperations.newPosting(mediaFileOwner));
            if (mediaFile.getFileSize() < OcrSpace.MAX_FILE_SIZE) {
                mediaFile.setRecognizeAt(Util.now());
            }

            return PrivateMediaFileInfoUtil.build(mediaFileOwner, null);
        } catch (InvalidImageException e) {
            throw new ValidationFailure("media.image-invalid");
        } catch (ThresholdReachedException e) {
            throw new ValidationFailure("media.wrong-size");
        } catch (IOException e) {
            throw new OperationFailure("media.storage-error");
        } finally {
            Files.deleteIfExists(tmp.path());
        }
    }

    private MediaFile getMediaFile(String id) {
        if (id.endsWith("=")) { // backward compatibility
            id = id.substring(0, id.length() - 1);
        }
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
        Principal viewPrincipal = mediaFileOwner.getViewE(requestContext.nodeName());
        if (
            !requestContext.isPrincipal(viewPrincipal, Scope.VIEW_MEDIA)
            && !feedOperations.isSheriffAllowed(() -> mediaOperations.getParentStories(id), viewPrincipal)
        ) {
            throw new ObjectNotFoundFailure("media.not-found");
        }
        return mediaFileOwner;
    }

    @GetMapping("/public/{id}/info")
    @Transactional
    public PublicMediaFileInfo getInfoPublic(@PathVariable String id) {
        log.info("GET /media/public/{id}/info (id = {})", LogUtil.format(id));

        return PublicMediaFileInfoUtil.build(getMediaFile(id));
    }

    @GetMapping("/private/{id}/info")
    @Transactional
    public PrivateMediaFileInfo getInfoPrivate(@PathVariable UUID id) {
        log.info("GET /media/private/{id}/info (id = {})", LogUtil.format(id));

        return PrivateMediaFileInfoUtil.build(getMediaFileOwner(id), null);
    }

    @GetMapping("/public/{id}/data")
    @Transactional
    public ResponseEntity<Resource> getDataPublic(
        @PathVariable String id,
        @RequestParam(required = false) Integer width,
        @RequestParam(required = false) Boolean download
    ) {
        log.info("GET /media/public/{id}/data (id = {})", LogUtil.format(id));

        return mediaOperations.serve(getMediaFile(id), width, download);
    }

    @GetMapping("/private/{id}/data")
    @Transactional
    public ResponseEntity<Resource> getDataPrivate(
        @PathVariable UUID id,
        @RequestParam(required = false) Integer width,
        @RequestParam(required = false) Boolean download
    ) {
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
            EntryInfo info = new EntryInfo();
            if (entry instanceof Posting posting) {
                info.setPosting(PostingInfoUtil.build(posting, entryOperations, requestContext));
            } else if (entry instanceof Comment comment) {
                info.setComment(CommentInfoUtil.build(comment, entryOperations, requestContext));
            } else {
                continue;
            }
            parents.add(info);
        }

        return parents;
    }

}
