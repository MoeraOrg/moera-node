package org.moera.node.operations;

import java.util.Comparator;
import java.util.Set;
import java.util.UUID;
import jakarta.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.lib.node.types.MediaAttachment;
import org.moera.lib.util.LogUtil;
import org.moera.node.data.EntryAttachment;
import org.moera.node.data.EntryAttachmentRepository;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.EntryRevisionRepository;
import org.moera.node.model.MediaAttachmentUtil;
import org.moera.node.task.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheMediaAttachmentsJob extends Job<CacheMediaAttachmentsJob.Parameters, Object> {

    private static final Logger log = LoggerFactory.getLogger(CacheMediaAttachmentsJob.class);

    public static class Parameters {

        private UUID entryRevisionId;
        private String receiverName;

        public Parameters() {
        }

        public Parameters(UUID entryRevisionId, String receiverName) {
            this.entryRevisionId = entryRevisionId;
            this.receiverName = receiverName;
        }

        public UUID getEntryRevisionId() {
            return entryRevisionId;
        }

        public void setEntryRevisionId(UUID entryRevisionId) {
            this.entryRevisionId = entryRevisionId;
        }

        public String getReceiverName() {
            return receiverName;
        }

        public void setReceiverName(String receiverName) {
            this.receiverName = receiverName;
        }

    }

    @Inject
    private EntryRevisionRepository entryRevisionRepository;

    @Inject
    private EntryAttachmentRepository entryAttachmentRepository;

    @Inject
    private ObjectMapper objectMapper;

    public CacheMediaAttachmentsJob() {
        retryCount(3, "PT10S");
    }

    @Override
    protected void setParameters(String parameters, ObjectMapper objectMapper) throws JsonProcessingException {
        this.parameters = objectMapper.readValue(parameters, Parameters.class);
    }

    @Override
    protected void setState(String state, ObjectMapper objectMapper) throws JsonProcessingException {
        this.state = null;
    }

    @Override
    protected void started() {
        super.started();
        log.debug("Caching media attachments for entry revision {}, receiver {}",
                LogUtil.format(parameters.entryRevisionId), LogUtil.format(parameters.receiverName));
    }

    @Override
    protected void execute() {
        tx.executeWrite(() -> {
            EntryRevision revision = entryRevisionRepository.findById(parameters.entryRevisionId).orElse(null);
            if (revision == null) {
                success();
            }

            MediaAttachmentsCache cache = null;
            if (revision.getAttachmentsCache() != null) {
                try {
                    cache = objectMapper.readValue(revision.getAttachmentsCache(), MediaAttachmentsCache.class);
                } catch (JsonProcessingException e) {
                    // ignore
                }
            }
            if (cache == null) {
                cache = new MediaAttachmentsCache();
            }

            Set<EntryAttachment> attachments = entryAttachmentRepository.findByEntryRevision(revision.getId());
            MediaAttachment[] mediaAttachments = attachments.stream()
                    .sorted(Comparator.comparingInt(EntryAttachment::getOrdinal))
                    .map(ea -> MediaAttachmentUtil.build(ea, parameters.receiverName))
                    .toArray(MediaAttachment[]::new);
            cache.putCache(parameters.receiverName, mediaAttachments);

            try {
                revision.setAttachmentsCache(objectMapper.writeValueAsString(cache));
            } catch (JsonProcessingException e) {
                log.error("Error serializing media attachments cache", e);
            }
        });
    }

}
