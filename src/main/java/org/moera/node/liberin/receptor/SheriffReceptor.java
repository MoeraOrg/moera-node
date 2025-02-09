package org.moera.node.liberin.receptor;

import jakarta.inject.Inject;

import org.moera.node.data.Comment;
import org.moera.node.data.Posting;
import org.moera.node.data.SheriffOrder;
import org.moera.node.instant.SheriffInstants;
import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.RemoteSheriffOrderReceivedLiberin;
import org.moera.node.liberin.model.SheriffOrderReceivedLiberin;
import org.moera.node.liberin.model.SheriffOrderSentLiberin;
import org.moera.node.model.event.CommentUpdatedEvent;
import org.moera.node.model.event.PostingUpdatedEvent;
import org.moera.node.model.notification.PostingUpdatedNotification;
import org.moera.node.model.notification.SheriffOrderForCommentAddedNotification;
import org.moera.node.model.notification.SheriffOrderForCommentDeletedNotification;
import org.moera.node.model.notification.SheriffOrderForPostingAddedNotification;
import org.moera.node.model.notification.SheriffOrderForPostingDeletedNotification;
import org.moera.node.notification.send.Directions;

@LiberinReceptor
public class SheriffReceptor extends LiberinReceptorBase {

    @Inject
    private SheriffInstants sheriffInstants;

    @LiberinMapping
    public void orderSent(SheriffOrderSentLiberin liberin) {
        SheriffOrder order = liberin.getSheriffOrder();
        if (order.getRemotePostingId() == null) {
            return;
        }
        if (order.getRemoteCommentId() == null) {
            send(Directions.single(liberin.getNodeId(), order.getRemotePostingOwnerName()),
                    !order.isDelete()
                            ? new SheriffOrderForPostingAddedNotification(order.getRemoteNodeName(),
                                    order.getRemoteFeedName(), order.getRemotePostingHeading(),
                                    order.getRemotePostingId(), order.getId().toString())
                            : new SheriffOrderForPostingDeletedNotification(order.getRemoteNodeName(),
                                    order.getRemoteFeedName(), order.getRemotePostingHeading(),
                                    order.getRemotePostingId(), order.getId().toString()));
        } else {
            send(Directions.single(liberin.getNodeId(), order.getRemoteCommentOwnerName()),
                    !order.isDelete()
                            ? new SheriffOrderForCommentAddedNotification(order.getRemoteNodeName(),
                                    order.getRemoteFeedName(), order.getRemotePostingOwnerName(),
                                    order.getRemotePostingOwnerFullName(), order.getRemotePostingHeading(),
                                    order.getRemotePostingId(), order.getRemoteCommentHeading(),
                                    order.getRemoteCommentId(), order.getId().toString())
                            : new SheriffOrderForCommentDeletedNotification(order.getRemoteNodeName(),
                                    order.getRemoteFeedName(), order.getRemotePostingOwnerName(),
                                    order.getRemotePostingOwnerFullName(), order.getRemotePostingHeading(),
                                    order.getRemotePostingId(), order.getRemoteCommentHeading(),
                                    order.getRemoteCommentId(), order.getId().toString()));
        }
    }

    @LiberinMapping
    public void orderReceived(SheriffOrderReceivedLiberin liberin) {
        Posting posting = liberin.getPosting();
        Comment comment = liberin.getComment();
        if (posting == null) {
            if (!liberin.isDeleted()) {
                sheriffInstants.orderForFeed(liberin.getFeedName(), liberin.getSheriffName(),
                        liberin.getSheriffAvatar(), liberin.getOrderId());
            } else {
                sheriffInstants.deletedOrderForFeed(
                        liberin.getFeedName(), liberin.getSheriffName(), liberin.getSheriffAvatar(),
                        liberin.getOrderId());
            }
        } else {
            if (comment == null) {
                send(liberin, new PostingUpdatedEvent(posting, posting.getViewE()));
                send(Directions.postingSubscribers(posting.getNodeId(), posting.getId(), posting.getViewE()),
                        new PostingUpdatedNotification(posting.getId()));
            } else {
                send(liberin, new CommentUpdatedEvent(comment,
                        posting.getViewE().a().and(posting.getViewCommentsE()).and(comment.getViewE())));
            }
        }
    }

    @LiberinMapping
    public void remoteOrderReceived(RemoteSheriffOrderReceivedLiberin liberin) {
        if (liberin.getCommentId() == null) {
            if (!liberin.isDeleted()) {
                sheriffInstants.orderForPosting(liberin.getRemoteNodeName(), liberin.getRemoteFeedName(),
                        liberin.getPostingHeading(), liberin.getPostingId(), liberin.getSheriffName(),
                        liberin.getSheriffAvatar(), liberin.getOrderId());
            } else {
                sheriffInstants.deletedOrderForPosting(liberin.getRemoteNodeName(), liberin.getRemoteFeedName(),
                        liberin.getPostingHeading(), liberin.getPostingId(), liberin.getSheriffName(),
                        liberin.getSheriffAvatar(), liberin.getOrderId());
            }
        } else {
            if (!liberin.isDeleted()) {
                sheriffInstants.orderForComment(liberin.getRemoteNodeName(), liberin.getRemoteFeedName(),
                        liberin.getPostingOwnerName(), liberin.getPostingOwnerFullName(), liberin.getPostingHeading(),
                        liberin.getPostingId(), liberin.getCommentHeading(), liberin.getCommentId(),
                        liberin.getSheriffName(), liberin.getSheriffAvatar(), liberin.getOrderId());
            } else {
                sheriffInstants.deletedOrderForComment(liberin.getRemoteNodeName(), liberin.getRemoteFeedName(),
                        liberin.getPostingOwnerName(), liberin.getPostingOwnerFullName(), liberin.getPostingHeading(),
                        liberin.getPostingId(), liberin.getCommentHeading(), liberin.getCommentId(),
                        liberin.getSheriffName(), liberin.getSheriffAvatar(), liberin.getOrderId());
            }
        }
    }

}
