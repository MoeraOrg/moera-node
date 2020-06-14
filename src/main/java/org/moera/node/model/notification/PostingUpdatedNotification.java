package org.moera.node.model.notification;

import java.util.UUID;

public class PostingUpdatedNotification extends SubscriberNotification {

    private String postingId;

    public PostingUpdatedNotification() {
        super(NotificationType.POSTING_UPDATED);
    }

    public PostingUpdatedNotification(UUID postingId) {
        super(NotificationType.POSTING_UPDATED);
        this.postingId = postingId.toString();
    }

    public String getPostingId() {
        return postingId;
    }

    public void setPostingId(String postingId) {
        this.postingId = postingId;
    }

}
