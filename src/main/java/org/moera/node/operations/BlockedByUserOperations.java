package org.moera.node.operations;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.moera.lib.node.types.BlockedOperation;
import org.moera.lib.node.types.RemotePostingOrNode;
import org.moera.node.data.BlockedByUser;
import org.moera.node.data.QBlockedByUser;
import org.moera.node.data.QContact;
import org.moera.node.global.RequestContext;
import org.moera.node.model.RemotePostingOrNodeUtil;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class BlockedByUserOperations {

    @Inject
    private RequestContext requestContext;

    @Inject
    private EntityManager entityManager;

    public List<BlockedByUser> search(
        UUID nodeId,
        List<BlockedOperation> blockedOperations,
        List<RemotePostingOrNode> postingsOrNodes,
        boolean strict
    ) {
        QBlockedByUser blockedByUser = QBlockedByUser.blockedByUser;
        QContact contact = QContact.contact;
        Predicate where = buildFilter(nodeId, blockedOperations, postingsOrNodes, strict);
        return new JPAQueryFactory(entityManager)
                .selectFrom(blockedByUser)
                .leftJoin(blockedByUser.contact, contact).fetchJoin()
                .leftJoin(contact.remoteAvatarMediaFile).fetchJoin()
                .where(where)
                .fetch();
    }

    private static BooleanBuilder buildFilter(
        UUID nodeId,
        List<BlockedOperation> blockedOperations,
        List<RemotePostingOrNode> postingsOrNodes,
        boolean strict
    ) {
        QBlockedByUser blockedByUser = QBlockedByUser.blockedByUser;
        BooleanBuilder where = new BooleanBuilder();
        where.and(blockedByUser.nodeId.eq(nodeId));
        if (blockedOperations != null) {
            where.and(blockedByUser.blockedOperation.in(blockedOperations));
        }
        if (!ObjectUtils.isEmpty(postingsOrNodes)) {
            BooleanBuilder wherePostings = new BooleanBuilder();
            for (RemotePostingOrNode postingOrNode : postingsOrNodes) {
                if (!ObjectUtils.isEmpty(postingOrNode.getNodeName())) {
                    if (ObjectUtils.isEmpty(postingOrNode.getPostingId()) || !strict) {
                        BooleanBuilder expr = new BooleanBuilder();
                        expr.and(blockedByUser.remoteNodeName.eq(postingOrNode.getNodeName()));
                        expr.and(blockedByUser.remotePostingId.isNull());
                        wherePostings.or(expr);
                    }
                    if (!ObjectUtils.isEmpty(postingOrNode.getPostingId())) {
                        BooleanBuilder expr = new BooleanBuilder();
                        expr.and(blockedByUser.remoteNodeName.eq(postingOrNode.getNodeName()));
                        expr.and(blockedByUser.remotePostingId.eq(postingOrNode.getPostingId()));
                        wherePostings.or(expr);
                    }
                }
            }
            where.and(wherePostings);
        } else if (strict) {
            where.and(blockedByUser.remoteNodeName.isNull())
                    .and(blockedByUser.remotePostingId.isNull());
        }
        return where;
    }

    public List<BlockedOperation> findBlockedOperations(String nodeName, String postingId) {
        return search(
            requestContext.nodeId(),
            List.of(BlockedOperation.COMMENT, BlockedOperation.REACTION),
            List.of(RemotePostingOrNodeUtil.build(nodeName, postingId)),
            false
        ).stream().map(BlockedByUser::getBlockedOperation).collect(Collectors.toList());
    }

}
