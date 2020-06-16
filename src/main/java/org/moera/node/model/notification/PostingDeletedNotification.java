package org.moera.node.model.notification;

import java.util.UUID;

public class PostingDeletedNotification extends PostingSubscriberNotification {

    public PostingDeletedNotification() {
        super(NotificationType.POSTING_DELETED);
    }

    public PostingDeletedNotification(UUID postingId) {
        super(NotificationType.POSTING_DELETED, postingId.toString());
    }

}
