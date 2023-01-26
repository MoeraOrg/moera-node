package org.moera.node.operations;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.inject.Inject;
import javax.transaction.Transactional;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.moera.node.data.BlockedOperation;
import org.moera.node.data.BlockedUser;
import org.moera.node.data.BlockedUserRepository;
import org.moera.node.data.QBlockedUser;
import org.moera.node.global.RequestContext;
import org.moera.node.global.UniversalContext;
import org.moera.node.util.Util;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BlockedUserOperations {

    @Inject
    private RequestContext requestContext;

    @Inject
    private UniversalContext universalContext;

    @Inject
    private BlockedUserRepository blockedUserRepository;

    @Inject
    private ContactOperations contactOperations;

    public Collection<BlockedUser> findExact(
            UUID nodeId, BlockedOperation blockedOperation, String remoteNodeName, UUID entryId, String entryNodeName,
            String entryPostingId
    ) {
        if (entryId != null) {
            return blockedUserRepository.findByOperationAndNodeAndEntry(
                    nodeId, blockedOperation, remoteNodeName, entryId);
        } else if (entryNodeName != null) {
            return blockedUserRepository.findByOperationAndNodeAndRemoteEntry(
                    nodeId, blockedOperation, remoteNodeName, entryNodeName, entryPostingId);
        } else {
            return blockedUserRepository.findByOperationAndNodeAndNoEntry(nodeId, blockedOperation, remoteNodeName);
        }
    }

    public Stream<BlockedUser> search(
            UUID nodeId, BlockedOperation blockedOperation, String remoteNodeName, UUID entryId, String entryNodeName,
            String entryPostingId
    ) {
        Predicate where = buildFilter(
                nodeId, blockedOperation, remoteNodeName, entryId, entryNodeName, entryPostingId);
        return StreamSupport.stream(blockedUserRepository.findAll(where).spliterator(), false);
    }

    public long count(UUID nodeId, BlockedOperation blockedOperation, String remoteNodeName, UUID entryId,
                      String entryNodeName, String entryPostingId) {
        Predicate where = buildFilter(
                nodeId, blockedOperation, remoteNodeName, entryId, entryNodeName, entryPostingId);
        return blockedUserRepository.count(where);
    }

    public boolean isBlocked(UUID nodeId, BlockedOperation blockedOperation, String remoteNodeName, UUID entryId,
                             String entryNodeName, String entryPostingId) {
        return count(nodeId, blockedOperation, remoteNodeName, entryId, entryNodeName, entryPostingId) > 0;
    }

    public boolean isBlocked(BlockedOperation blockedOperation, UUID entryId, String entryNodeName,
                             String entryPostingId) {
        return isBlocked(requestContext.nodeId(), blockedOperation, requestContext.getClientName(), entryId,
                entryNodeName, entryPostingId);
    }

    public boolean isBlocked(BlockedOperation blockedOperation, UUID entryId) {
        return isBlocked(requestContext.nodeId(), blockedOperation, requestContext.getClientName(), entryId,
                null, null);
    }

    public boolean isBlocked(BlockedOperation blockedOperation) {
        return isBlocked(requestContext.nodeId(), blockedOperation, requestContext.getClientName(), null,
                null, null);
    }

    private static BooleanBuilder buildFilter(
            UUID nodeId, BlockedOperation blockedOperation, String remoteNodeName, UUID entryId, String entryNodeName,
            String entryPostingId
    ) {
        QBlockedUser blockedUser = QBlockedUser.blockedUser;
        BooleanBuilder where = new BooleanBuilder();
        where.and(blockedUser.nodeId.eq(nodeId));
        if (blockedOperation != null) {
            where.and(blockedUser.blockedOperation.eq(blockedOperation));
        }
        if (remoteNodeName != null) {
            where.and(blockedUser.remoteNodeName.eq(remoteNodeName));
        }
        if (entryId != null) {
            BooleanBuilder expr = new BooleanBuilder();
            expr.or(blockedUser.entry.id.eq(entryId))
                    .or(blockedUser.entry.isNull());
            where.and(expr);
        } else {
            where.and(blockedUser.entry.isNull());
        }
        if (entryNodeName != null) {
            BooleanBuilder expr = new BooleanBuilder();
            expr.or(blockedUser.entryNodeName.eq(entryNodeName))
                    .or(blockedUser.entryNodeName.isNull());
            where.and(expr);
        } else {
            where.and(blockedUser.entryNodeName.isNull());
        }
        if (entryPostingId != null) {
            BooleanBuilder expr = new BooleanBuilder();
            expr.or(blockedUser.entryPostingId.eq(entryPostingId))
                    .or(blockedUser.entryPostingId.isNull());
            where.and(expr);
        } else {
            where.and(blockedUser.entryPostingId.isNull());
        }
        return where;
    }

    @Scheduled(fixedDelayString = "PT1H")
    @Transactional
    public void purgeExpired() {
        blockedUserRepository.findExpired(Util.now()).forEach(blockedUser -> {
            universalContext.associate(blockedUser.getNodeId());
            contactOperations.updateBlockedUserCounts(blockedUser, -1);
            blockedUserRepository.delete(blockedUser);
        });
    }

}
