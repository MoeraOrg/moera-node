package org.moera.node.model.notification;

public class SheriffOrderForCommentAddedNotification extends SheriffOrderForCommentNotification {

    public SheriffOrderForCommentAddedNotification() {
        super(NotificationType.SHERIFF_ORDER_FOR_COMMENT_ADDED);
    }

    public SheriffOrderForCommentAddedNotification(String remoteNodeName, String remoteFeedName,
                                                   String postingOwnerName, String postingOwnerFullName,
                                                   String postingHeading, String postingId, String commentHeading,
                                                   String commentId, String orderId) {
        super(NotificationType.SHERIFF_ORDER_FOR_COMMENT_ADDED, remoteNodeName, remoteFeedName, postingOwnerName,
                postingOwnerFullName, postingHeading, postingId, commentHeading, commentId, orderId);
    }

}
