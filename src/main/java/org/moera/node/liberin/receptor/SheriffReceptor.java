package org.moera.node.liberin.receptor;

import javax.inject.Inject;

import org.moera.node.data.Comment;
import org.moera.node.data.Posting;
import org.moera.node.instant.SheriffInstants;
import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.RemoteSheriffOrderReceivedLiberin;
import org.moera.node.liberin.model.SheriffOrderReceivedLiberin;
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
    public void orderReceived(SheriffOrderReceivedLiberin liberin) {
        Posting posting = liberin.getPosting();
        Comment comment = liberin.getComment();
        if (posting == null) {
            if (!liberin.isDeleted()) {
                sheriffInstants.orderForFeed(liberin.getFeedName(), liberin.getSheriffName(), liberin.getOrderId());
            } else {
                sheriffInstants.deletedOrderForFeed(
                        liberin.getFeedName(), liberin.getSheriffName(), liberin.getOrderId());
            }
        } else {
            if (comment == null) {
                send(liberin, new PostingUpdatedEvent(posting, posting.getViewE()));
                send(Directions.single(posting.getNodeId(), posting.getOwnerName()),
                        !liberin.isDeleted()
                                ? new SheriffOrderForPostingAddedNotification(universalContext.nodeName(),
                                        liberin.getFeedName(), posting.getCurrentRevision().getHeading(),
                                        posting.getId().toString(), liberin.getOrderId())
                                : new SheriffOrderForPostingDeletedNotification(universalContext.nodeName(),
                                        liberin.getFeedName(), posting.getCurrentRevision().getHeading(),
                                        posting.getId().toString(), liberin.getOrderId()));
                send(Directions.postingSubscribers(posting.getNodeId(), posting.getId(), posting.getViewE()),
                        new PostingUpdatedNotification(posting.getId()));
            } else {
                send(liberin, new CommentUpdatedEvent(comment,
                        posting.getViewE().a().and(posting.getViewCommentsE()).and(comment.getViewE())));
                send(Directions.single(comment.getNodeId(), comment.getOwnerName()),
                        !liberin.isDeleted()
                                ? new SheriffOrderForCommentAddedNotification(universalContext.nodeName(),
                                        liberin.getFeedName(), posting.getOwnerName(), posting.getOwnerFullName(),
                                        posting.getCurrentRevision().getHeading(), posting.getId().toString(),
                                        comment.getCurrentRevision().getHeading(), comment.getId().toString(),
                                        liberin.getOrderId())
                                : new SheriffOrderForCommentDeletedNotification(universalContext.nodeName(),
                                        liberin.getFeedName(), posting.getOwnerName(), posting.getOwnerFullName(),
                                        posting.getCurrentRevision().getHeading(), posting.getId().toString(),
                                        comment.getCurrentRevision().getHeading(), comment.getId().toString(),
                                        liberin.getOrderId()));
            }
        }
    }

    @LiberinMapping
    public void remoteOrderReceived(RemoteSheriffOrderReceivedLiberin liberin) {
        if (liberin.getCommentId() == null) {
            if (!liberin.isDeleted()) {
                sheriffInstants.orderForPosting(liberin.getRemoteNodeName(), liberin.getRemoteFeedName(),
                        liberin.getPostingHeading(), liberin.getPostingId(), liberin.getSheriffName(),
                        liberin.getOrderId());
            } else {
                sheriffInstants.deletedOrderForPosting(liberin.getRemoteNodeName(), liberin.getRemoteFeedName(),
                        liberin.getPostingHeading(), liberin.getPostingId(), liberin.getSheriffName(),
                        liberin.getOrderId());
            }
        } else {
            if (!liberin.isDeleted()) {
                sheriffInstants.orderForComment(liberin.getRemoteNodeName(), liberin.getRemoteFeedName(),
                        liberin.getPostingOwnerName(), liberin.getPostingOwnerFullName(), liberin.getPostingHeading(),
                        liberin.getPostingId(), liberin.getCommentHeading(), liberin.getCommentId(),
                        liberin.getSheriffName(), liberin.getOrderId());
            } else {
                sheriffInstants.deletedOrderForComment(liberin.getRemoteNodeName(), liberin.getRemoteFeedName(),
                        liberin.getPostingOwnerName(), liberin.getPostingOwnerFullName(), liberin.getPostingHeading(),
                        liberin.getPostingId(), liberin.getCommentHeading(), liberin.getCommentId(),
                        liberin.getSheriffName(), liberin.getOrderId());
            }
        }
    }

}
