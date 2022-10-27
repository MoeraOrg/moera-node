package org.moera.node.operations;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import javax.inject.Inject;

import org.moera.node.data.EntryRevisionRepository;
import org.moera.node.data.EntryType;
import org.moera.node.domain.Domains;
import org.moera.node.util.ExtendedDuration;
import org.moera.node.util.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

@Component
public class EntryOperations {

    private static final Logger log = LoggerFactory.getLogger(EntryOperations.class);

    @Inject
    private Domains domains;

    @Inject
    private EntryRevisionRepository entryRevisionRepository;

    @Inject
    private PlatformTransactionManager txManager;

    @Scheduled(fixedDelayString = "P1D")
    public void purgeOutdatedRevisions() {
        try {
            for (String domainName : domains.getAllDomainNames()) {
                UUID nodeId = domains.getDomainNodeId(domainName);
                purgeOutdatedRevisions(nodeId, domainName, EntryType.POSTING, true, "posting.revision.lifetime");
                purgeOutdatedRevisions(nodeId, domainName, EntryType.POSTING, false, "posting.picked.revision.lifetime");
                purgeOutdatedRevisions(nodeId, domainName, EntryType.COMMENT, true, "comment.revision.lifetime");
            }
        } catch (Throwable e) {
            log.error("Error purging outdated entry revisions", e);
        }
    }

    private void purgeOutdatedRevisions(UUID nodeId, String domainName, EntryType entryType, boolean original,
                                        String optionName) throws Throwable {
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
            log.info("Purging revisions of entry {}", entryId);
            Transaction.execute(txManager, () -> {
                entryRevisionRepository.deleteOutdated(entryId, createdBefore);
                entryRevisionRepository.updateTotalRevisions(entryId);
                return null;
            });
        }
    }

}
