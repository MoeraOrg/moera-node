package org.moera.node.model.notification;

public class SheriffOrderForCommentDeletedNotification extends SheriffOrderForCommentNotification {

    public SheriffOrderForCommentDeletedNotification() {
        super(NotificationType.SHERIFF_ORDER_FOR_COMMENT_DELETED);
    }

    public SheriffOrderForCommentDeletedNotification(String remoteNodeName, String remoteFeedName,
                                                     String postingOwnerName, String postingOwnerFullName,
                                                     String postingHeading, String postingId, String commentHeading,
                                                     String commentId, String orderId) {
        super(NotificationType.SHERIFF_ORDER_FOR_COMMENT_DELETED, remoteNodeName, remoteFeedName, postingOwnerName,
                postingOwnerFullName, postingHeading, postingId, commentHeading, commentId, orderId);
    }

}
