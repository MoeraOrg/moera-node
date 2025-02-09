package org.moera.node.operations;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Comparator;
import java.util.Set;
import java.util.UUID;
import jakarta.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.node.data.EntryAttachment;
import org.moera.node.data.EntryAttachmentRepository;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.EntryRevisionRepository;
import org.moera.node.data.EntryType;
import org.moera.node.domain.Domains;
import org.moera.node.global.RequestCounter;
import org.moera.node.model.MediaAttachment;
import org.moera.node.model.MediaAttachmentsCache;
import org.moera.node.task.Jobs;
import org.moera.node.util.ExtendedDuration;
import org.moera.node.util.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EntryOperations implements MediaAttachmentsProvider {

    private static final Logger log = LoggerFactory.getLogger(EntryOperations.class);

    @Inject
    private RequestCounter requestCounter;

    @Inject
    private Domains domains;

    @Inject
    private EntryRevisionRepository entryRevisionRepository;

    @Inject
    private EntryAttachmentRepository entryAttachmentRepository;

    @Inject
    private Transaction tx;

    @Inject
    private Jobs jobs;

    @Inject
    private ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "P1D")
    public void purgeOutdatedRevisions() {
        try (var ignored = requestCounter.allot()) {
            log.info("Purging outdated revisions");

            for (String domainName : domains.getAllDomainNames()) {
                UUID nodeId = domains.getDomainNodeId(domainName);
                purgeOutdatedRevisions(nodeId, domainName, EntryType.POSTING, true, "posting.revision.lifetime");
                purgeOutdatedRevisions(nodeId, domainName, EntryType.POSTING, false, "posting.picked.revision.lifetime");
                purgeOutdatedRevisions(nodeId, domainName, EntryType.COMMENT, true, "comment.revision.lifetime");
            }
        }
    }

    private void purgeOutdatedRevisions(UUID nodeId, String domainName, EntryType entryType, boolean original,
                                        String optionName) {
        MDC.put("domain", domainName);

        ExtendedDuration lifetime = domains.getDomainOptions(domainName).getDuration(optionName);
        if (lifetime.isNever()) {
            return;
        }
        Timestamp createdBefore = Timestamp.from(Instant.now().minus(lifetime.getDuration()));
        Set<UUID> entryIds = original
                ? entryRevisionRepository.findOriginalEntriesWithOutdated(nodeId, entryType, createdBefore)
                : entryRevisionRepository.findNotOriginalEntriesWithOutdated(nodeId, entryType, createdBefore);
        for (UUID entryId : entryIds) {
            log.info("Purging outdated revisions of entry {}", entryId);
            tx.executeWrite(() -> {
                entryRevisionRepository.deleteOutdated(entryId, createdBefore);
                entryRevisionRepository.updateTotalRevisions(entryId);
            });
        }
    }

    public MediaAttachment[] getMediaAttachments(EntryRevision revision, String receiverName) {
        try {
            if (revision.getAttachmentsCache() != null) {
                var data = objectMapper.readValue(revision.getAttachmentsCache(), MediaAttachmentsCache.class);
                MediaAttachment[] cache = data.getCache(receiverName);
                if (cache != null) {
                    return cache;
                }
            }
        } catch (JsonProcessingException e) {
            log.error("Error parsing media attachments cache", e);
            // fetch from the database
        }

        jobs.runNoPersist(
                CacheMediaAttachmentsJob.class,
                new CacheMediaAttachmentsJob.Parameters(revision.getId(), receiverName));

        Set<EntryAttachment> attachments = entryAttachmentRepository.findByEntryRevision(revision.getId());
        return attachments.stream()
                .sorted(Comparator.comparingInt(EntryAttachment::getOrdinal))
                .map(ea -> new MediaAttachment(ea, receiverName))
                .toArray(MediaAttachment[]::new);
    }

}
