package org.moera.node.notification.model;

import java.util.UUID;

import org.moera.node.notification.NotificationType;

public abstract class MentionPostingNotification extends Notification {

    private String postingId;

    protected MentionPostingNotification(NotificationType type) {
        super(type);
    }

    public MentionPostingNotification(NotificationType type, UUID postingId) {
        super(type);
        this.postingId = postingId.toString();
    }

    public String getPostingId() {
        return postingId;
    }

    public void setPostingId(String postingId) {
        this.postingId = postingId;
    }

}
