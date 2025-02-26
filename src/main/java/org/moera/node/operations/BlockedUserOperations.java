package org.moera.node.operations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.moera.lib.node.types.BlockedOperation;
import org.moera.lib.node.types.Scope;
import org.moera.node.data.BlockedUser;
import org.moera.node.data.BlockedUserRepository;
import org.moera.node.data.QBlockedUser;
import org.moera.node.data.QContact;
import org.moera.node.domain.Domains;
import org.moera.node.global.RequestContext;
import org.moera.node.global.RequestCounter;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.Liberin;
import org.moera.node.liberin.LiberinManager;
import org.moera.node.liberin.model.BlockedUserDeletedLiberin;
import org.moera.node.util.Transaction;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class BlockedUserOperations {

    private static final Logger log = LoggerFactory.getLogger(BlockedUserOperations.class);

    @Inject
    private RequestCounter requestCounter;

    @Inject
    private RequestContext requestContext;

    @Inject
    private UniversalContext universalContext;

    @Inject
    private Domains domains;

    @Inject
    private BlockedUserRepository blockedUserRepository;

    @Inject
    private ContactOperations contactOperations;

    @Inject
    @Lazy
    private LiberinManager liberinManager;

    @Inject
    private EntityManager entityManager;

    @Inject
    private Transaction tx;

    private static <E> List<E> single(E e) {
        return e != null ? List.of(e) : null;
    }

    public List<BlockedUser> search(
        UUID nodeId,
        Collection<BlockedOperation> blockedOperations,
        String remoteNodeName,
        UUID entryId,
        String entryNodeName,
        String entryPostingId,
        boolean strict
    ) {
        return search(nodeId, blockedOperations, remoteNodeName, single(entryId), entryNodeName, entryPostingId, strict);
    }

    public List<BlockedUser> search(
        UUID nodeId,
        Collection<BlockedOperation> blockedOperations,
        String remoteNodeName,
        Collection<UUID> entryIds,
        String entryNodeName,
        String entryPostingId,
        boolean strict
    ) {
        QBlockedUser blockedUser = QBlockedUser.blockedUser;
        QContact contact = QContact.contact;
        Predicate where = buildFilter(
            nodeId,
            blockedOperations,
            remoteNodeName,
            entryIds,
            entryNodeName,
            entryPostingId,
            strict
        );
        return new JPAQueryFactory(entityManager)
            .selectFrom(blockedUser)
            .leftJoin(blockedUser.contact, contact).fetchJoin()
            .leftJoin(contact.remoteAvatarMediaFile).fetchJoin()
            .where(where)
            .fetch();
    }

    public long count(
        UUID nodeId,
        Collection<BlockedOperation> blockedOperations,
        String remoteNodeName,
        UUID entryId,
        String entryNodeName,
        String entryPostingId,
        boolean strict
    ) {
        Predicate where = buildFilter(
            nodeId,
            blockedOperations,
            remoteNodeName,
            single(entryId),
            entryNodeName,
            entryPostingId,
            strict
        );
        return blockedUserRepository.count(where);
    }

    public boolean isBlocked(
        UUID nodeId,
        Collection<BlockedOperation> blockedOperations,
        String remoteNodeName,
        UUID entryId,
        String entryNodeName,
        String entryPostingId
    ) {
        if (remoteNodeName == null) {
            return false;
        }
        return count(nodeId, blockedOperations, remoteNodeName, entryId, entryNodeName, entryPostingId, false) > 0;
    }

    public boolean isBlocked(
        Collection<BlockedOperation> blockedOperations,
        UUID entryId,
        String entryNodeName,
        String entryPostingId
    ) {
        return isBlocked(
            requestContext.nodeId(),
            blockedOperations,
            requestContext.getClientName(Scope.IDENTIFY),
            entryId,
            entryNodeName,
            entryPostingId
        );
    }

    public boolean isBlocked(BlockedOperation blockedOperation, UUID entryId) {
        return isBlocked(
            requestContext.nodeId(),
            List.of(blockedOperation),
            requestContext.getClientName(Scope.IDENTIFY),
            entryId,
            null,
            null
        );
    }

    public boolean isBlocked(Collection<BlockedOperation> blockedOperations, UUID entryId) {
        return isBlocked(
            requestContext.nodeId(),
            blockedOperations,
            requestContext.getClientName(Scope.IDENTIFY),
            entryId,
            null,
            null
        );
    }

    public boolean isBlocked(BlockedOperation... blockedOperations) {
        return isBlocked(
            requestContext.nodeId(),
            Arrays.asList(blockedOperations),
            requestContext.getClientName(Scope.IDENTIFY),
            null,
            null,
            null
        );
    }

    private static BooleanBuilder buildFilter(
        UUID nodeId,
        Collection<BlockedOperation> blockedOperations,
        String remoteNodeName,
        Collection<UUID> entryIds,
        String entryNodeName,
        String entryPostingId,
        boolean strict
    ) {
        QBlockedUser blockedUser = QBlockedUser.blockedUser;
        BooleanBuilder where = new BooleanBuilder();
        where.and(blockedUser.nodeId.eq(nodeId));
        if (blockedOperations != null) {
            where.and(blockedUser.blockedOperation.in(blockedOperations));
        }
        if (remoteNodeName != null) {
            where.and(blockedUser.remoteNodeName.eq(remoteNodeName));
        }
        if (!ObjectUtils.isEmpty(entryIds)) {
            BooleanBuilder expr = new BooleanBuilder();
            expr.or(blockedUser.entry.id.in(entryIds));
            if (!strict) {
                expr.or(blockedUser.entry.isNull());
            }
            where.and(expr);
        } else {
            where.and(blockedUser.entry.isNull());
        }
        if (entryNodeName != null) {
            BooleanBuilder expr = new BooleanBuilder();
            expr.or(blockedUser.entryNodeName.eq(entryNodeName));
            if (!strict) {
                expr.or(blockedUser.entryNodeName.isNull());
            }
            where.and(expr);
        } else {
            where.and(blockedUser.entryNodeName.isNull());
        }
        if (entryPostingId != null) {
            BooleanBuilder expr = new BooleanBuilder();
            expr.or(blockedUser.entryPostingId.eq(entryPostingId));
            if (!strict) {
                expr.or(blockedUser.entryPostingId.isNull());
            }
            where.and(expr);
        } else {
            where.and(blockedUser.entryPostingId.isNull());
        }
        return where;
    }

    public List<BlockedOperation> findBlockedOperations(UUID postingId) {
        String clientName = requestContext.getClientName(Scope.IDENTIFY);
        if (ObjectUtils.isEmpty(clientName) || requestContext.isOwner()) {
            return Collections.emptyList();
        }
        return search(
            requestContext.nodeId(),
            List.of(BlockedOperation.COMMENT, BlockedOperation.REACTION),
            clientName,
            postingId,
            null,
            null,
            false
        ).stream().map(BlockedUser::getBlockedOperation).collect(Collectors.toList());
    }

    public void recalculateChecksums(UUID nodeId) {
        int checksum = Arrays.hashCode(blockedUserRepository.findIdsByGlobalOperation(
                nodeId, BlockedOperation.VISIBILITY).toArray(UUID[]::new));
        domains.getDomainOptions(nodeId).set("blocked-users.visibility.checksum", checksum);
    }

    @Scheduled(fixedDelayString = "PT1H")
    public void purgeExpired() {
        try (var ignored = requestCounter.allot()) {
            log.info("Purging expired blockings of users");

            List<Liberin> liberinList = new ArrayList<>();
            tx.executeWrite(() -> {
                Set<UUID> nodeIds = new HashSet<>();
                blockedUserRepository.findExpired(Util.now()).forEach(blockedUser -> {
                    nodeIds.add(blockedUser.getNodeId());
                    universalContext.associate(blockedUser.getNodeId());
                    contactOperations.updateBlockedUserCounts(blockedUser, -1).fill(blockedUser);
                    blockedUserRepository.delete(blockedUser);
                    liberinList.add(new BlockedUserDeletedLiberin(blockedUser).withNodeId(blockedUser.getNodeId()));
                });
                nodeIds.forEach(this::recalculateChecksums);
            });
            liberinList.forEach(liberinManager::send);
        }
    }

}
