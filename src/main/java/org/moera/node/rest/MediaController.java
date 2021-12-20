package org.moera.node.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.AuthenticationException;
import org.moera.node.config.Config;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.MediaFileOwnerRepository;
import org.moera.node.data.MediaFileRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.RequestContext;
import org.moera.node.media.InvalidImageException;
import org.moera.node.media.MediaOperations;
import org.moera.node.media.MediaPathNotSetException;
import org.moera.node.media.ThresholdReachedException;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.PrivateMediaFileInfo;
import org.moera.node.model.PublicMediaFileInfo;
import org.moera.node.model.ValidationFailure;
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
import org.springframework.web.bind.annotation.ResponseBody;

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
    private MediaOperations mediaOperations;

    @Inject
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
        int maxSize = requestContext.getOptions().getInt("posting.media.max-size");
        return MediaOperations.transfer(in, out, contentLength, maxSize);
    }

    private Optional<MediaFileOwner> findMediaFileOwnerByFile(String id) {
        return requestContext.isAdmin()
                ? mediaFileOwnerRepository.findByAdminFile(requestContext.nodeId(), id)
                : mediaFileOwnerRepository.findByFile(requestContext.nodeId(), requestContext.getClientName(), id);
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

        var tmp = mediaOperations.tmpFile();
        try {
            DigestingOutputStream out = transfer(in, tmp.getOutputStream(), contentLength);
            String id = out.getHash();
            MediaFileOwner mediaFileOwner = findMediaFileOwnerByFile(id).orElse(null);
            if (mediaFileOwner != null) {
                return new PrivateMediaFileInfo(mediaFileOwner);
            }

            MediaFile mediaFile = mediaOperations.putInPlace(
                    id, toContentType(mediaType), tmp.getPath(), out.getDigest());
            mediaFile = entityManager.merge(mediaFile); // entity is detached after putInPlace() transaction closed
            mediaFileOwner = mediaOperations.own(mediaFile,
                    requestContext.isAdmin() ? null : requestContext.getClientName());

            return new PrivateMediaFileInfo(mediaFileOwner);
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
        return mediaFileOwnerRepository.findFullById(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("media.not-found"));
    }

    @GetMapping("/public/{id}/info")
    @Transactional
    public PublicMediaFileInfo getInfoPublic(@PathVariable String id) {
        log.info("GET /media/public/{id}/info (id = {})", LogUtil.format(id));

        return new PublicMediaFileInfo(getMediaFile(id));
    }

    @GetMapping("/private/{id}/info")
    @Transactional
    public PrivateMediaFileInfo getInfoPrivate(@PathVariable UUID id) {
        log.info("GET /media/private/{id}/info (id = {})", LogUtil.format(id));

        return new PrivateMediaFileInfo(getMediaFileOwner(id));
    }

    @GetMapping("/public/{id}/data")
    @Transactional
    @ResponseBody
    public ResponseEntity<Resource> getDataPublic(@PathVariable String id,
                                                  @RequestParam(required = false) Integer width) {
        log.info("GET /media/public/{id}/data (id = {})", LogUtil.format(id));

        return mediaOperations.serve(getMediaFile(id), width);
    }

    @GetMapping("/private/{id}/data")
    @Transactional
    public ResponseEntity<Resource> getDataPrivate(@PathVariable UUID id,
                                                   @RequestParam(required = false) Integer width) {
        log.info("GET /media/private/{id}/data (id = {})", LogUtil.format(id));

        return mediaOperations.serve(getMediaFileOwner(id).getMediaFile(), width);
    }

}
