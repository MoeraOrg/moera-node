package org.moera.node.model.notification;

import java.util.UUID;

public class PostingUpdatedNotification extends PostingSubscriberNotification {

    public PostingUpdatedNotification() {
        super(NotificationType.POSTING_UPDATED);
    }

    public PostingUpdatedNotification(UUID postingId) {
        super(NotificationType.POSTING_UPDATED, postingId.toString());
    }

}
