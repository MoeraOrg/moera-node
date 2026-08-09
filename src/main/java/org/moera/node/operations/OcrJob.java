package org.moera.node.operations;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import jakarta.inject.Inject;

import org.moera.lib.util.LogUtil;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileOwnerRepository;
import org.moera.node.data.MediaFileRepository;
import org.moera.node.liberin.model.MediaRecognizedTextUpdatedLiberin;
import org.moera.node.media.MediaFileNotAvailableException;
import org.moera.node.ocrspace.OcrSpace;
import org.moera.node.ocrspace.OcrSpaceConnectionException;
import org.moera.node.ocrspace.OcrSpaceInvalidResponseException;
import org.moera.node.task.Job;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;
import tools.jackson.databind.ObjectMapper;

public class OcrJob extends Job<OcrJob.Parameters, Object> {

    public static class Parameters {

        private String mediaFileId;

        public Parameters() {
        }

        public Parameters(String mediaFileId) {
            this.mediaFileId = mediaFileId;
        }

        public String getMediaFileId() {
            return mediaFileId;
        }

        public void setMediaFileId(String mediaFileId) {
            this.mediaFileId = mediaFileId;
        }

    }

    private static final Logger log = LoggerFactory.getLogger(OcrJob.class);

    @Inject
    private MediaFileRepository mediaFileRepository;

    @Inject
    private MediaFileOwnerRepository mediaFileOwnerRepository;

    @Inject
    private OcrOperations ocrOperations;

    @Inject
    private OcrSpace ocrSpace;

    public OcrJob() {
        exponentialRetry("PT1H", "P7D");
    }

    @Override
    protected void setParameters(String parameters, ObjectMapper objectMapper) {
        this.parameters = objectMapper.readValue(parameters, OcrJob.Parameters.class);
    }

    @Override
    protected void setState(String state, ObjectMapper objectMapper) {
        this.state = null;
    }

    @Override
    protected void started() {
        super.started();
        log.info("Starting OCR of media file {}", LogUtil.format(parameters.mediaFileId));
    }

    @Override
    protected void execute() {
        MediaFile mediaFile = tx.executeRead(() ->
            mediaFileRepository.findById(parameters.mediaFileId).orElse(null)
        );
        if (mediaFile == null) {
            log.warn("Media file {} not found", LogUtil.format(parameters.mediaFileId));
            success();
        }

        try {
            String text = ocrSpace.recognize(mediaFile);
            tx.executeWrite(() -> mediaFileRepository.recognized(parameters.mediaFileId, text, Util.now()));
            if (!ObjectUtils.isEmpty(text)) {
                tx.executeWrite(() -> updateText(mediaFile, text));
            }
        } catch (MediaFileNotAvailableException e) {
            log.warn("Media file {} has no local copy", LogUtil.format(parameters.mediaFileId));
            success();
        } catch (OcrSpaceConnectionException | OcrSpaceInvalidResponseException e) {
            log.error("Error during OCR of media file {}: {}", LogUtil.format(parameters.mediaFileId), e.getMessage());
            retry();
        }
    }

    private void updateText(MediaFile mediaFile, String text) {
        var owners = mediaFileOwnerRepository.findAllByFile(mediaFile.getId());
        for (var owner : owners) {
            universalContext.associate(owner.getNodeId());
            ocrOperations.update(owner, text);
            send(new MediaRecognizedTextUpdatedLiberin(owner.getId(), text));
        }
    }

    @Override
    protected void succeeded() {
        super.succeeded();
        log.info("Succeeded OCR of media file {}", LogUtil.format(parameters.mediaFileId));
    }

    @Override
    protected void failed() {
        super.failed();
        log.info("Failed OCR of media file {}", LogUtil.format(parameters.mediaFileId));
        tx.executeWriteQuietly(() ->
            mediaFileRepository.assignRecognizeAt(
                parameters.mediaFileId, Timestamp.from(Instant.now().plus(31, ChronoUnit.DAYS))
            )
        );
    }

}
