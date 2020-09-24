package org.moera.node.model.notification;

import java.util.UUID;

public abstract class CommentReactionNotification extends Notification {

    private String postingId;
    private String commentId;
    private String ownerName;
    private boolean negative;

    protected CommentReactionNotification(NotificationType type) {
        super(type);
    }

    public CommentReactionNotification(NotificationType type, UUID postingId, UUID commentId, String ownerName,
                                       boolean negative) {
        super(type);
        this.postingId = postingId.toString();
        this.commentId = commentId.toString();
        this.ownerName = ownerName;
        this.negative = negative;
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

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public boolean isNegative() {
        return negative;
    }

    public void setNegative(boolean negative) {
        this.negative = negative;
    }

}
