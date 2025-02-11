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
import org.moera.node.data.BlockedByUser;
import org.moera.node.data.QBlockedByUser;
import org.moera.node.data.QContact;
import org.moera.node.global.RequestContext;
import org.moera.node.model.RemotePosting;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class BlockedByUserOperations {

    @Inject
    private RequestContext requestContext;

    @Inject
    private EntityManager entityManager;

    public List<BlockedByUser> search(UUID nodeId, BlockedOperation[] blockedOperations, RemotePosting[] postings,
                                      boolean strict) {
        QBlockedByUser blockedByUser = QBlockedByUser.blockedByUser;
        QContact contact = QContact.contact;
        Predicate where = buildFilter(nodeId, blockedOperations, postings, strict);
        return new JPAQueryFactory(entityManager)
                .selectFrom(blockedByUser)
                .leftJoin(blockedByUser.contact, contact).fetchJoin()
                .leftJoin(contact.remoteAvatarMediaFile).fetchJoin()
                .where(where)
                .fetch();
    }

    private static BooleanBuilder buildFilter(UUID nodeId, BlockedOperation[] blockedOperations,
                                              RemotePosting[] postings, boolean strict) {
        QBlockedByUser blockedByUser = QBlockedByUser.blockedByUser;
        BooleanBuilder where = new BooleanBuilder();
        where.and(blockedByUser.nodeId.eq(nodeId));
        if (blockedOperations != null) {
            where.and(blockedByUser.blockedOperation.in(blockedOperations));
        }
        if (!ObjectUtils.isEmpty(postings)) {
            BooleanBuilder wherePostings = new BooleanBuilder();
            for (RemotePosting posting : postings) {
                if (!ObjectUtils.isEmpty(posting.getNodeName())) {
                    if (ObjectUtils.isEmpty(posting.getPostingId()) || !strict) {
                        BooleanBuilder expr = new BooleanBuilder();
                        expr.and(blockedByUser.remoteNodeName.eq(posting.getNodeName()));
                        expr.and(blockedByUser.remotePostingId.isNull());
                        wherePostings.or(expr);
                    }
                    if (!ObjectUtils.isEmpty(posting.getPostingId())) {
                        BooleanBuilder expr = new BooleanBuilder();
                        expr.and(blockedByUser.remoteNodeName.eq(posting.getNodeName()));
                        expr.and(blockedByUser.remotePostingId.eq(posting.getPostingId()));
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
                new BlockedOperation[]{BlockedOperation.COMMENT, BlockedOperation.REACTION},
                new RemotePosting[]{new RemotePosting(nodeName, postingId)},
                false
        ).stream().map(BlockedByUser::getBlockedOperation).collect(Collectors.toList());
    }

}
