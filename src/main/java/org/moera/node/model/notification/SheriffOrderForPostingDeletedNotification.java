package org.moera.node.model.notification;

public class SheriffOrderForPostingDeletedNotification extends SheriffOrderForPostingNotification {

    public SheriffOrderForPostingDeletedNotification() {
        super(NotificationType.SHERIFF_ORDER_FOR_POSTING_DELETED);
    }

    public SheriffOrderForPostingDeletedNotification(String remoteNodeName, String remoteFeedName, String postingHeading,
                                                     String postingId, String orderId) {
        super(NotificationType.SHERIFF_ORDER_FOR_POSTING_DELETED, remoteNodeName, remoteFeedName, postingHeading,
                postingId, orderId);
    }

}
