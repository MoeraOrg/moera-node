package org.moera.node.rest;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.moera.lib.node.types.MediaUploadAttributes;
import org.moera.lib.node.types.MediaUploadInfo;
import org.moera.lib.node.types.Result;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.validate.ValidationFailure;
import org.moera.lib.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.data.MediaUpload;
import org.moera.node.data.MediaUploadRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.RequestContext;
import org.moera.node.media.MediaUploadOperations;
import org.moera.node.model.MediaUploadInfoUtil;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.OperationFailure;
import org.moera.node.util.Transaction;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/media/upload")
public class MediaUploadController {

    private static final Logger log = LoggerFactory.getLogger(MediaUploadController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private MediaUploadRepository mediaUploadRepository;

    @Inject
    private MediaUploadOperations mediaUploadOperations;

    @Inject
    private Transaction tx;

    @PostMapping
    @Admin(Scope.UPLOAD_PRIVATE_MEDIA)
    @Transactional
    public MediaUploadInfo create(@RequestBody MediaUploadAttributes attributes) {
        log.info(
            "POST /media/upload (mimeType = {}, fileSize = {}, chunkSize = {})",
            LogUtil.format(attributes.getMimeType()),
            LogUtil.format(attributes.getFileSize()),
            LogUtil.format(attributes.getChunkSize())
        );

        attributes.validate();

        int maxMediaSize = requestContext.getOptions().getInt("media.max-size");
        if (attributes.getFileSize() <= 0 || attributes.getFileSize() > maxMediaSize) {
            throw new ValidationFailure("media.wrong-size");
        }

        int maxChunkSize = requestContext.getOptions().getInt("media.upload.max-chunk-size");
        int chunkSize = mediaUploadOperations.selectChunkSize(attributes.getChunkSize(), maxChunkSize);

        MediaUpload mediaUpload = new MediaUpload();
        mediaUpload.setId(UUID.randomUUID());
        mediaUpload.setNodeId(requestContext.nodeId());
        mediaUpload.setMimeType(attributes.getMimeType());
        mediaUpload.setTitle(!ObjectUtils.isEmpty(attributes.getTitle()) ? attributes.getTitle() : null);
        mediaUpload.setFileSize(attributes.getFileSize());
        mediaUpload.setChunkSize(chunkSize);
        mediaUpload.setDeadline(Timestamp.from(Instant.now().plus(MediaUploadOperations.UPLOAD_TTL)));
        mediaUpload = mediaUploadRepository.save(mediaUpload);

        try {
            mediaUploadOperations.createUploadFile(mediaUpload);
        } catch (IOException e) {
            mediaUploadOperations.deleteUploadFileQuietly(mediaUpload);
            throw new OperationFailure("media.storage-error");
        }

        return MediaUploadInfoUtil.build(mediaUpload);
    }

    @GetMapping("/{id}")
    @Admin(Scope.UPLOAD_PRIVATE_MEDIA)
    @Transactional
    public MediaUploadInfo get(@PathVariable String id) {
        log.info("GET /media/upload/{id} (id = {})", LogUtil.format(id));

        UUID uploadId = Util.uuid(id).orElseThrow(() -> new ObjectNotFoundFailure("media-upload.not-found"));
        return MediaUploadInfoUtil.build(getMediaUpload(uploadId));
    }

    @PutMapping("/{id}/{chunk}")
    @Admin(Scope.UPLOAD_PRIVATE_MEDIA)
    public MediaUploadInfo put(
        @PathVariable String id,
        @PathVariable int chunk,
        @RequestHeader(value = "Content-Length", required = false) Long contentLength,
        InputStream in
    ) throws Exception {
        log.info(
            "PUT /media/upload/{id}/{chunk} (id = {}, chunk = {}, Content-Length = {})",
            LogUtil.format(id), LogUtil.format(chunk), LogUtil.format(contentLength)
        );

        UUID uploadId = Util.uuid(id).orElseThrow(() -> new ObjectNotFoundFailure("media-upload.not-found"));
        try (var ignored = mediaUploadOperations.lock(uploadId)) {
            return tx.executeWriteWithExceptions(() -> {
                MediaUpload mediaUpload = getMediaUpload(uploadId);
                mediaUploadOperations.writeChunk(mediaUpload, chunk, in, contentLength);
                return MediaUploadInfoUtil.build(mediaUpload);
            });
        } catch (IOException e) {
            throw new OperationFailure("media.storage-error");
        }
    }

    @DeleteMapping("/{id}")
    @Admin(Scope.UPLOAD_PRIVATE_MEDIA)
    public Result delete(@PathVariable String id) throws Exception {
        log.info("DELETE /media/upload/{id} (id = {})", LogUtil.format(id));

        UUID uploadId = Util.uuid(id).orElseThrow(() -> new ObjectNotFoundFailure("media-upload.not-found"));
        try (var ignored = mediaUploadOperations.lock(uploadId)) {
            tx.executeWriteWithExceptions(() -> {
                MediaUpload mediaUpload = getMediaUpload(uploadId);
                try {
                    mediaUploadOperations.deleteUploadFile(mediaUpload);
                } catch (IOException e) {
                    throw new OperationFailure("media.storage-error");
                }
                mediaUploadRepository.deleteByNodeIdAndId(requestContext.nodeId(), uploadId);
            });
        }

        return Result.OK;
    }

    private MediaUpload getMediaUpload(UUID id) {
        return mediaUploadRepository.findByNodeIdAndId(requestContext.nodeId(), id)
            .orElseThrow(() -> new ObjectNotFoundFailure("media-upload.not-found"));
    }

}
