package org.moera.node.rest;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.Objects;
import java.util.UUID;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import org.moera.lib.node.types.BlockedOperation;
import org.moera.lib.node.types.PrivateMediaFileAttributes;
import org.moera.lib.node.types.PrivateMediaFileInfo;
import org.moera.lib.node.types.PublicMediaFileInfo;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.validate.ValidationFailure;
import org.moera.lib.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.auth.AuthenticationException;
import org.moera.node.auth.UserBlockedException;
import org.moera.node.config.Config;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.MediaFileOwnerRepository;
import org.moera.node.data.MediaFileRepository;
import org.moera.node.data.MediaUpload;
import org.moera.node.data.MediaUploadRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.model.MediaTitleUpdatedLiberin;
import org.moera.node.media.InvalidImageException;
import org.moera.node.media.MediaGrantGenerator;
import org.moera.node.media.MediaGrantProperties;
import org.moera.node.media.MediaGrantSupplier;
import org.moera.node.media.MediaGrantValidator;
import org.moera.node.media.MediaOperations;
import org.moera.node.media.MediaUploadOperations;
import org.moera.node.media.MimeUtil;
import org.moera.node.media.ThresholdReachedException;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.PrivateMediaFileInfoUtil;
import org.moera.node.model.PublicMediaFileInfoUtil;
import org.moera.node.ocrspace.OcrSpace;
import org.moera.node.operations.BlockedUserOperations;
import org.moera.node.util.DigestingOutputStream;
import org.moera.node.util.ExtendedDuration;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private MediaUploadRepository mediaUploadRepository;

    @Inject
    private MediaOperations mediaOperations;

    @Inject
    private MediaUploadOperations mediaUploadOperations;

    @Inject
    private BlockedUserOperations blockedUserOperations;

    @Inject
    private MediaGrantValidator mediaGrantValidator;

    @Inject
    @PersistenceContext
    private EntityManager entityManager;

    // like mediaType.toString(), but without charset or any other parameter
    private String toContentType(MediaType mediaType) {
        return mediaType != null ? mediaType.getType() + "/" + mediaType.getSubtype() : null;
    }

    private String contentFileName(String contentDisposition) {
        try {
            return contentDisposition != null ? ContentDisposition.parse(contentDisposition).getFilename() : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String uploadedFileName(String contentType, String fileName) {
        return !MimeUtil.isSupportedImage(contentType) && fileName != null
            ? StringUtils.stripFilenameExtension(StringUtils.getFilename(fileName))
            : null;
    }

    private DigestingOutputStream transfer(
        InputStream in, OutputStream out, Long contentLength, Integer maxSize
    ) throws IOException {
        if (maxSize == null) {
            maxSize = requestContext.getOptions().getInt("media.max-size");
        }
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

        boolean admin = requestContext.isAdmin(Scope.UPLOAD_PUBLIC_MEDIA);
        if (!admin && requestContext.getClientName(Scope.UPLOAD_PUBLIC_MEDIA) == null) {
            throw new AuthenticationException();
        }
        if (isBlocked()) {
            throw new UserBlockedException();
        }

        var tmp = mediaOperations.tmpFile();
        Integer maxSize = admin ? null : requestContext.getOptions().getInt("avatar.max-size");
        try {
            DigestingOutputStream out = transfer(in, tmp.outputStream(), contentLength, maxSize);
            MediaFile mediaFile = mediaOperations.putInPlace(
                out.getHash(), toContentType(mediaType), tmp.path(), out.getDigest(), true
            );
            mediaFile = mediaFileRepository.save(mediaFile);

            return PublicMediaFileInfoUtil.build(mediaFile, config.getMedia().getDirectServe());
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

    @PostMapping("/private")
    @Admin(Scope.UPLOAD_PRIVATE_MEDIA)
    @Transactional
    public PrivateMediaFileInfo postPrivate(
        @RequestHeader(value = "Content-Type", required = false) MediaType mediaType,
        @RequestHeader(value = "Content-Length", required = false) Long contentLength,
        @RequestHeader(value = "Content-Disposition", required = false) String contentDisposition,
        @RequestParam(required = false) String upload,
        @RequestParam(required = false, defaultValue = "false") boolean downsize,
        InputStream in
    ) {
        log.info(
            "POST /media/private (Content-Type: {}, Content-Length: {}, upload = {}, downsize = {})",
            LogUtil.format(Objects.toString(mediaType, null)),
            LogUtil.format(contentLength),
            LogUtil.format(upload),
            LogUtil.format(downsize)
        );

        try {
            MediaFileOwner mediaFileOwner = ObjectUtils.isEmpty(upload)
                ? ownMediaFromBody(in, mediaType, contentLength, contentDisposition, downsize)
                : ownMediaFromUpload(upload, downsize);
            if (isSuitableForOcr(mediaFileOwner.getMediaFile())) {
                mediaFileOwner.getMediaFile().setRecognizeAt(Util.now());
            }
            return PrivateMediaFileInfoUtil.build(
                mediaFileOwner, config.getMedia().getDirectServe(), new MediaGrantGenerator(requestContext.getOptions())
            );
        } catch (InvalidImageException e) {
            throw new ValidationFailure("media.image-invalid");
        } catch (ThresholdReachedException e) {
            throw new ValidationFailure("media.wrong-size");
        } catch (IOException e) {
            throw new OperationFailure("media.storage-error");
        }
    }

    private MediaFileOwner ownMediaFromBody(
        InputStream in,
        MediaType mediaType,
        Long contentLength,
        String contentDisposition,
        boolean downsize
    ) throws IOException {
        var tmp = mediaOperations.tmpFile();
        try {
            DigestingOutputStream out = transfer(in, tmp.outputStream(), contentLength, null);
            String contentType = toContentType(mediaType);
            if (downsize) {
                contentType = downsizeImage(tmp.path(), contentType);
                try (InputStream tmpIn = new FileInputStream(tmp.path().toFile())) {
                    out = transfer(tmpIn, null, null, null);
                }
            }

            MediaFile mediaFile = mediaOperations.putInPlace(
                out.getHash(), contentType, tmp.path(), out.getDigest(), false
            );
            // the entity is detached after putInPlace() transaction closed
            mediaFile = entityManager.merge(mediaFile);
            String fileName = uploadedFileName(contentType, contentFileName(contentDisposition));
            return mediaOperations.own(mediaFile, fileName);
        } finally {
            Files.deleteIfExists(tmp.path());
        }
    }

    private MediaFileOwner ownMediaFromUpload(String id, boolean downsize) throws IOException {
        UUID uploadId = Util.uuid(id).orElseThrow(() -> new ObjectNotFoundFailure("media-upload.not-found"));
        try (var ignored = mediaUploadOperations.lock(uploadId)) {
            MediaUpload mediaUpload = mediaUploadRepository.findByNodeIdAndId(requestContext.nodeId(), uploadId)
                .orElseThrow(() -> new ObjectNotFoundFailure("media-upload.not-found"));
            if (!mediaUpload.isCompleted()) {
                throw new OperationFailure("media-upload.not-completed");
            }
            Path path = mediaUploadOperations.getPath(mediaUpload);
            String contentType = mediaUpload.getMimeType();
            if (downsize) {
                contentType = downsizeImage(path, contentType);
            }
            DigestingOutputStream out;
            try (InputStream tmpIn = new FileInputStream(path.toFile())) {
                out = transfer(tmpIn, null, null, null);
            }

            MediaFile mediaFile = mediaOperations.putInPlace(
                out.getHash(), contentType, path, out.getDigest(), false
            );
            // the entity is detached after putInPlace() transaction closed
            mediaFile = entityManager.merge(mediaFile);
            String fileName = uploadedFileName(contentType, mediaUpload.getTitle());
            MediaFileOwner mediaFileOwner = mediaOperations.own(mediaFile, fileName);
            // if the file already exists, the uploaded one will not be moved out
            mediaUploadOperations.deleteUploadFileQuietly(mediaUpload);
            mediaUploadRepository.deleteByNodeIdAndId(requestContext.nodeId(), uploadId);

            return mediaFileOwner;
        }
    }

    private String downsizeImage(Path path, String contentType) throws IOException {
        return mediaOperations.downsizeImage(
            path, contentType, requestContext.getOptions().getInt("media.image.recommended-size")
        );
    }

    private boolean isSuitableForOcr(MediaFile mediaFile) {
        return (mediaFile.isImage() || MediaType.APPLICATION_PDF_VALUE.equals(mediaFile.getMimeType()))
            && mediaFile.getFileSize() < OcrSpace.MAX_FILE_SIZE;
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

    private MediaFileOwner getMediaFileOwner(UUID id, MediaGrantProperties grant) {
        MediaFileOwner mediaFileOwner = mediaFileOwnerRepository.findFullById(requestContext.nodeId(), id)
            .orElseThrow(() -> new ObjectNotFoundFailure("media.not-found"));
        if (
            grant == null
            && !mediaFileOwner.isUnrestricted()
            && !requestContext.isAdmin(Scope.VIEW_CONTENT)
        ) {
            throw new ObjectNotFoundFailure("media.not-found");
        }
        return mediaFileOwner;
    }

    @GetMapping("/public/{id}/info")
    @Transactional
    public PublicMediaFileInfo getInfoPublic(@PathVariable String id) {
        log.info("GET /media/public/{id}/info (id = {})", LogUtil.format(id));

        return PublicMediaFileInfoUtil.build(getMediaFile(id), config.getMedia().getDirectServe());
    }

    @GetMapping("/private/{id}/info")
    @Transactional
    public PrivateMediaFileInfo getInfoPrivate(
        @PathVariable String id,
        @RequestParam(name = "grant", required = false) String grantS
    ) {
        log.info("GET /media/private/{id}/info (id = {})", LogUtil.format(id));

        UUID mediaId = Util.uuid(id).orElseThrow(() -> new ObjectNotFoundFailure("media.not-found"));
        MediaGrantProperties grant = mediaGrantValidator.validate(grantS, mediaId);

        MediaGrantSupplier grantSupplier = null;
        if (grantS != null) {
            var expires = mediaGrantValidator.getExpires(grantS);
            grantSupplier = new MediaGrantSupplier() {

                @Override
                public String generateLocal(
                    String mediaId, ExtendedDuration duration, boolean download, String fileName
                ) {
                    return grantS;
                }

                @Override
                public String generateRemote(
                    String mediaId, ExtendedDuration duration, boolean download, String fileName
                ) {
                    return grantS;
                }

                @Override
                public Timestamp expires(ExtendedDuration duration) {
                    return expires;
                }

            };
        } else if (requestContext.isAdmin(Scope.VIEW_CONTENT)) {
            grantSupplier = new MediaGrantGenerator(requestContext.getOptions());
        }

        return PrivateMediaFileInfoUtil.build(
            getMediaFileOwner(mediaId, grant), config.getMedia().getDirectServe(), grantSupplier
        );
    }

    @PutMapping("/private/{id}/info")
    @Admin(Scope.UPLOAD_PRIVATE_MEDIA)
    @Transactional
    public PrivateMediaFileInfo updatePrivateMediaInfo(
        @PathVariable String id,
        @RequestBody PrivateMediaFileAttributes attributes
    ) {
        log.info("PUT /media/private/{id}/info (id = {})", LogUtil.format(id));

        attributes.validate();

        UUID mediaId = Util.uuid(id).orElseThrow(() -> new ObjectNotFoundFailure("media.not-found"));
        MediaFileOwner mediaFileOwner = mediaFileOwnerRepository.findFullById(requestContext.nodeId(), mediaId)
            .orElseThrow(() -> new ObjectNotFoundFailure("media.not-found"));

        if (attributes.getTitle() != null) {
            mediaFileOwner.setTitle(attributes.getTitle().isEmpty() ? null : attributes.getTitle());
            mediaOperations.mediaTextUpdated(mediaFileOwner);
            requestContext.send(new MediaTitleUpdatedLiberin(mediaFileOwner.getId(), mediaFileOwner.getTitle()));
        }

        return PrivateMediaFileInfoUtil.build(
            mediaFileOwner, config.getMedia().getDirectServe(), new MediaGrantGenerator(requestContext.getOptions())
        );
    }

    @GetMapping("/public/{id}/data")
    @Transactional
    public ResponseEntity<Resource> getDataPublic(
        @PathVariable String id,
        @RequestParam(required = false) Integer width,
        @RequestParam(required = false) Boolean download
    ) {
        log.info("GET /media/public/{id}/data (id = {})", LogUtil.format(id));

        return mediaOperations.serve(getMediaFile(id), width, null, download, ExtendedDuration.ALWAYS);
    }

    @GetMapping("/private/{id}/data")
    @Transactional
    public ResponseEntity<Resource> getDataPrivate(
        @PathVariable String id,
        @RequestParam(required = false) Integer width,
        @RequestParam(required = false) Boolean download,
        @RequestParam(name = "grant", required = false) String grantS,
        @RequestParam(name = "ignoremalware", required = false) Boolean ignoreMalware
    ) {
        log.info("GET /media/private/{id}/data (id = {})", LogUtil.format(id));

        UUID mediaId = Util.uuid(id).orElseThrow(() -> new ObjectNotFoundFailure("media.not-found"));
        MediaGrantProperties grant = mediaGrantValidator.validate(grantS, mediaId);
        MediaFileOwner mediaFileOwner = getMediaFileOwner(mediaId, grant);
        mediaOperations.blockMalware(mediaFileOwner, ignoreMalware);

        return mediaOperations.serve(
            mediaFileOwner.getMediaFile(),
            width,
            mediaFileOwner.getUserFileName(),
            download,
            MediaGrantProperties.cacheDuration(grant, mediaFileOwner.isUnrestricted())
        );
    }

}
