package org.moera.node.notification.model;

import org.moera.node.notification.NotificationType;

public abstract class MentionPostingNotification extends Notification {

    private String postingId;

    protected MentionPostingNotification(NotificationType type) {
        super(type);
    }

    public String getPostingId() {
        return postingId;
    }

    public void setPostingId(String postingId) {
        this.postingId = postingId;
    }

}
