package org.moera.node.operations;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.inject.Inject;

import org.moera.lib.node.types.MediaAttachment;
import org.moera.node.config.Config;
import org.moera.node.data.EntryAttachment;
import org.moera.node.data.EntryAttachmentRepository;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.EntryRevisionRepository;
import org.moera.node.data.EntryType;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.domain.Domains;
import org.moera.node.global.RequestCounter;
import org.moera.node.global.UniversalContext;
import org.moera.node.media.MediaGrantSupplier;
import org.moera.node.media.MediaOperations;
import org.moera.node.model.MediaAttachmentUtil;
import org.moera.node.model.MediaFilePreviewInfoUtil;
import org.moera.node.model.PrivateMediaFileInfoUtil;
import org.moera.node.task.Jobs;
import org.moera.node.util.ExtendedDuration;
import org.moera.node.util.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class EntryOperations implements MediaAttachmentsProvider {

    private static final Logger log = LoggerFactory.getLogger(EntryOperations.class);

    @Inject
    private Config config;

    @Inject
    private RequestCounter requestCounter;

    @Inject
    private UniversalContext universalContext;

    @Inject
    private Domains domains;

    @Inject
    private EntryRevisionRepository entryRevisionRepository;

    @Inject
    private EntryAttachmentRepository entryAttachmentRepository;

    @Inject
    private MediaOperations mediaOperations;

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
                universalContext.associate(domains.getDomainNodeId(domainName));
                purgeOutdatedRevisions(EntryType.POSTING, true, "posting.revision.lifetime");
                purgeOutdatedRevisions(EntryType.POSTING, false, "posting.picked.revision.lifetime");
                purgeOutdatedRevisions(EntryType.COMMENT, true, "comment.revision.lifetime");
            }
        }
    }

    private void purgeOutdatedRevisions(
        EntryType entryType, boolean original, String optionName
    ) {
        ExtendedDuration lifetime = universalContext.getOptions().getDuration(optionName);
        if (lifetime.isNever()) {
            return;
        }
        Timestamp createdBefore = Timestamp.from(Instant.now().minus(lifetime.getDuration()));
        Set<UUID> entryIds = original
            ? entryRevisionRepository.findOriginalEntriesWithOutdated(
                universalContext.nodeId(), entryType, createdBefore
            )
            : entryRevisionRepository.findNotOriginalEntriesWithOutdated(
                universalContext.nodeId(), entryType, createdBefore
            );
        for (UUID entryId : entryIds) {
            log.info("Purging outdated revisions of entry {}", entryId);
            tx.executeWrite(() -> {
                Set<MediaFileOwner> affectedMedia =
                    entryAttachmentRepository.findMediaByOutdatedRevisions(entryId, createdBefore);
                entryRevisionRepository.deleteOutdated(entryId, createdBefore);
                entryRevisionRepository.updateTotalRevisions(entryId);
                mediaOperations.deleteObsoleteMediaPostings(affectedMedia);
            });
        }
    }

    @Override
    public List<MediaAttachment> getMediaAttachments(EntryRevision revision, MediaGrantSupplier grantSupplier) {
        try {
            if (revision.getAttachmentsCache() != null) {
                var data = objectMapper.readValue(revision.getAttachmentsCache(), MediaAttachmentsCache.class);
                var cache = data.getAttachments();
                if (cache != null) {
                    // Media paths expire, so assuming that they always need to be updated
                    updateCachedPaths(cache, grantSupplier);
                    return cache;
                }
            }
        } catch (JacksonException e) {
            log.error("Error parsing media attachments cache", e);
            // fetch from the database
        }

        if (jobs.isReady()) {
            jobs.runNoPersist(CacheMediaAttachmentsJob.class, new CacheMediaAttachmentsJob.Parameters(revision.getId()));
        }

        Set<EntryAttachment> attachments = entryAttachmentRepository.findByEntryRevision(revision.getId());
        return attachments.stream()
            .sorted(Comparator.comparingInt(EntryAttachment::getOrdinal))
            .map(ea -> MediaAttachmentUtil.build(ea, config.getMedia().getDirectServe(), grantSupplier))
            .collect(Collectors.toList());
    }

    private void updateCachedPaths(List<MediaAttachment> attachments, MediaGrantSupplier grantSupplier) {
        for (var attachment : attachments) {
            if (attachment.getMedia() != null) {
                var media = attachment.getMedia();
                PrivateMediaFileInfoUtil.fillPath(media, grantSupplier);
                PrivateMediaFileInfoUtil.fillDirectPath(media, config.getMedia().getDirectServe());
                if (media.getPreviews() != null) {
                    for (var preview : media.getPreviews()) {
                        MediaFilePreviewInfoUtil.fillPath(preview, media, grantSupplier);
                        MediaFilePreviewInfoUtil.fillDirectPath(preview, config.getMedia().getDirectServe());
                    }
                }
            }
        }
    }

}
