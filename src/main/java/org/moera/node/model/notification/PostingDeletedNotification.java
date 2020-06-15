package org.moera.node.model.notification;

import java.util.UUID;

public class PostingDeletedNotification extends SubscriberNotification {

    private String postingId;

    public PostingDeletedNotification() {
        super(NotificationType.POSTING_DELETED);
    }

    public PostingDeletedNotification(UUID postingId) {
        super(NotificationType.POSTING_DELETED);
        this.postingId = postingId.toString();
    }

    public String getPostingId() {
        return postingId;
    }

    public void setPostingId(String postingId) {
        this.postingId = postingId;
    }

}
