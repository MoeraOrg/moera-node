package org.moera.node.operations;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import jakarta.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.lib.node.types.body.Body;
import org.moera.lib.util.LogUtil;
import org.moera.node.data.DraftRepository;
import org.moera.node.data.Entry;
import org.moera.node.data.EntryAttachment;
import org.moera.node.data.EntryRevisionRepository;
import org.moera.node.data.EntryType;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.MediaFileOwnerRepository;
import org.moera.node.data.MediaFileRepository;
import org.moera.node.liberin.model.CommentHeadingUpdatedLiberin;
import org.moera.node.liberin.model.CommentMediaTextUpdatedLiberin;
import org.moera.node.liberin.model.DraftUpdatedLiberin;
import org.moera.node.liberin.model.PostingHeadingUpdatedLiberin;
import org.moera.node.liberin.model.PostingMediaTextUpdatedLiberin;
import org.moera.node.ocrspace.OcrSpace;
import org.moera.node.ocrspace.OcrSpaceConnectionException;
import org.moera.node.ocrspace.OcrSpaceInvalidResponseException;
import org.moera.node.task.Job;
import org.moera.node.text.TextConverter;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private EntryRevisionRepository entryRevisionRepository;

    @Inject
    private DraftRepository draftRepository;

    @Inject
    private OcrSpace ocrSpace;

    public OcrJob() {
        exponentialRetry("PT1H", "P7D");
    }

    @Override
    protected void setParameters(String parameters, ObjectMapper objectMapper) throws JsonProcessingException {
        this.parameters = objectMapper.readValue(parameters, OcrJob.Parameters.class);
    }

    @Override
    protected void setState(String state, ObjectMapper objectMapper) throws JsonProcessingException {
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
            if (text != null) {
                tx.executeWrite(() -> updateText(mediaFile, text));
            }
        } catch (OcrSpaceConnectionException | OcrSpaceInvalidResponseException e) {
            log.error("Error during OCR of media file {}: {}", LogUtil.format(parameters.mediaFileId), e.getMessage());
            retry();
        }
    }

    private void updateText(MediaFile mediaFile, String text) {
        var owners = mediaFileOwnerRepository.findAllByFile(mediaFile.getId());
        for (var owner : owners) {
            universalContext.associate(owner.getNodeId());

            var revisions = entryRevisionRepository.findByMedia(owner.getId());
            for (var revision : revisions) {
                revision.setAttachmentsCache(null);
                Entry entry = revision.getEntry();
                boolean collapseQuotations = entry.getEntryType() == EntryType.COMMENT;
                List<MediaFileOwner> media = revision.getAttachments().stream()
                    .map(EntryAttachment::getMediaFileOwner)
                    .collect(Collectors.toList());
                String oldHeading = revision.getHeading();
                String oldDescription = revision.getDescription();
                TextConverter.headingToRevision(new Body(revision.getBody()), media, collapseQuotations, revision);
                switch (entry.getEntryType()) {
                    case POSTING:
                        send(new PostingMediaTextUpdatedLiberin(entry.getId(), owner.getId(), text));
                        break;
                    case COMMENT:
                        send(new CommentMediaTextUpdatedLiberin(entry.getId(), owner.getId(), text));
                        break;
                }
                if (
                    !Objects.equals(oldHeading, revision.getHeading())
                    || !Objects.equals(oldDescription, revision.getDescription())
                ) {
                    switch (entry.getEntryType()) {
                        case POSTING:
                            send(
                                new PostingHeadingUpdatedLiberin(
                                    entry.getId(), revision.getId(), revision.getHeading(), revision.getDescription()
                                )
                            );
                            break;
                        case COMMENT:
                            send(
                                new CommentHeadingUpdatedLiberin(entry.getId(), revision.getId(), revision.getHeading())
                            );
                            break;
                    }
                }
            }

            draftRepository.findByMedia(owner.getId()).forEach(draft -> send(new DraftUpdatedLiberin(draft)));
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
