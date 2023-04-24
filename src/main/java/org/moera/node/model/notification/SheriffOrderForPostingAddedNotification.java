package org.moera.node.model.notification;

public class SheriffOrderForPostingAddedNotification extends SheriffOrderForPostingNotification {

    public SheriffOrderForPostingAddedNotification() {
        super(NotificationType.SHERIFF_ORDER_FOR_POSTING_ADDED);
    }

    public SheriffOrderForPostingAddedNotification(String remoteNodeName, String remoteFeedName, String postingHeading,
                                                   String postingId, String orderId) {
        super(NotificationType.SHERIFF_ORDER_FOR_POSTING_ADDED, remoteNodeName, remoteFeedName, postingHeading,
                postingId, orderId);
    }

}
