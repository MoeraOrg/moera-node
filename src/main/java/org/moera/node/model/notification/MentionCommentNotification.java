package org.moera.node.model.notification;

import java.util.UUID;

public abstract class MentionCommentNotification extends Notification {

    private String postingId;
    private String commentId;

    protected MentionCommentNotification(NotificationType type) {
        super(type);
    }

    public MentionCommentNotification(NotificationType type, UUID postingId, UUID commentId) {
        super(type);
        this.postingId = postingId.toString();
        this.commentId = commentId.toString();
    }

    public String getPostingId() {
        return postingId;
    }

    public void setPostingId(String postingId) {
        this.postingId = postingId;
    }

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

}
