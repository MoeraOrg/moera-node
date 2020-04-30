package org.moera.node.notification.model;

public abstract class MentionPostingNotification extends Notification {

    private String postingId;

    public String getPostingId() {
        return postingId;
    }

    public void setPostingId(String postingId) {
        this.postingId = postingId;
    }

}
