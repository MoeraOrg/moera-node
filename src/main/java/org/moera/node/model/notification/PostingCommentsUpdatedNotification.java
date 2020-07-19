package org.moera.node.model.notification;

import java.util.UUID;

public class PostingCommentsUpdatedNotification extends PostingSubscriberNotification {

    private int total;

    public PostingCommentsUpdatedNotification() {
        super(NotificationType.POSTING_COMMENTS_UPDATED);
    }

    public PostingCommentsUpdatedNotification(UUID postingId, int total) {
        super(NotificationType.POSTING_COMMENTS_UPDATED, postingId.toString());
        this.total = total;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

}
