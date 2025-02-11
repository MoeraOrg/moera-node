package org.moera.node.operations;

import java.util.UUID;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.moera.lib.node.types.StoryType;
import org.moera.node.data.BlockedInstant;
import org.moera.node.data.BlockedInstantRepository;
import org.moera.node.data.QBlockedInstant;
import org.moera.node.global.RequestCounter;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BlockedInstantOperations {

    private static final Logger log = LoggerFactory.getLogger(BlockedInstantOperations.class);

    @Inject
    private RequestCounter requestCounter;

    @Inject
    private BlockedInstantRepository blockedInstantRepository;

    public Stream<BlockedInstant> findExact(
        UUID nodeId, StoryType storyType, UUID entryId, String remoteNodeName, String remotePostingId,
        String remoteOwnerName
    ) {
        BooleanBuilder where = buildExactFilter(
                nodeId, storyType, entryId, remoteNodeName, remotePostingId, remoteOwnerName);
        return StreamSupport.stream(blockedInstantRepository.findAll(where).spliterator(), false);
    }

    public Stream<BlockedInstant> search(
            UUID nodeId, StoryType storyType, UUID entryId, String remoteNodeName, String remotePostingId,
            String remoteOwnerName
    ) {
        Predicate where = buildFilter(
                nodeId, storyType, entryId, remoteNodeName, remotePostingId, remoteOwnerName);
        return StreamSupport.stream(blockedInstantRepository.findAll(where).spliterator(), false);
    }

    public long count(UUID nodeId, StoryType storyType) {
        return count(nodeId, storyType, null, null, null, null);
    }

    public long count(
            UUID nodeId, StoryType storyType, UUID entryId, String remoteNodeName, String remotePostingId,
            String remoteOwnerName
    ) {
        Predicate where = buildFilter(
                nodeId, storyType, entryId, remoteNodeName, remotePostingId, remoteOwnerName);
        return blockedInstantRepository.count(where);
    }

    private static BooleanBuilder buildExactFilter(
            UUID nodeId, StoryType storyType, UUID entryId, String remoteNodeName, String remotePostingId,
            String remoteOwnerName
    ) {
        QBlockedInstant blockedInstant = QBlockedInstant.blockedInstant;
        BooleanBuilder where = new BooleanBuilder();
        where.and(blockedInstant.nodeId.eq(nodeId))
                .and(blockedInstant.storyType.eq(storyType));
        if (entryId != null) {
            where.and(blockedInstant.entry.id.eq(entryId));
        } else {
            where.and(blockedInstant.entry.isNull());
        }
        if (remoteNodeName != null) {
            where.and(blockedInstant.remoteNodeName.eq(remoteNodeName));
        } else {
            where.and(blockedInstant.remoteNodeName.isNull());
        }
        if (remotePostingId != null) {
            where.and(blockedInstant.remotePostingId.eq(remotePostingId));
        } else {
            where.and(blockedInstant.remotePostingId.isNull());
        }
        if (remoteOwnerName != null) {
            where.and(blockedInstant.remoteOwnerName.eq(remoteOwnerName));
        } else {
            where.and(blockedInstant.remoteOwnerName.isNull());
        }
        return where;
    }

    private static BooleanBuilder buildFilter(
            UUID nodeId, StoryType storyType, UUID entryId, String remoteNodeName, String remotePostingId,
            String remoteOwnerName
    ) {
        QBlockedInstant blockedInstant = QBlockedInstant.blockedInstant;
        BooleanBuilder where = new BooleanBuilder();
        where.and(blockedInstant.nodeId.eq(nodeId))
                .and(blockedInstant.storyType.eq(storyType));
        if (entryId != null) {
            BooleanBuilder expr = new BooleanBuilder();
            expr.or(blockedInstant.entry.id.eq(entryId))
                    .or(blockedInstant.entry.isNull());
            where.and(expr);
        } else {
            where.and(blockedInstant.entry.isNull());
        }
        if (remoteNodeName != null) {
            BooleanBuilder expr = new BooleanBuilder();
            expr.or(blockedInstant.remoteNodeName.eq(remoteNodeName))
                    .or(blockedInstant.remoteNodeName.isNull());
            where.and(expr);
        } else {
            where.and(blockedInstant.remoteNodeName.isNull());
        }
        if (remotePostingId != null) {
            BooleanBuilder expr = new BooleanBuilder();
            expr.or(blockedInstant.remotePostingId.eq(remotePostingId))
                    .or(blockedInstant.remotePostingId.isNull());
            where.and(expr);
        } else {
            where.and(blockedInstant.remotePostingId.isNull());
        }
        if (remoteOwnerName != null) {
            BooleanBuilder expr = new BooleanBuilder();
            expr.or(blockedInstant.remoteOwnerName.eq(remoteOwnerName))
                    .or(blockedInstant.remoteOwnerName.isNull());
            where.and(expr);
        } else {
            where.and(blockedInstant.remoteOwnerName.isNull());
        }
        return where;
    }

    @Scheduled(fixedDelayString = "PT1H")
    @Transactional
    public void purgeExpired() {
        try (var ignored = requestCounter.allot()) {
            log.info("Purging expired blockings of instants");

            blockedInstantRepository.deleteExpired(Util.now());
        }
    }

}
