package org.moera.node.model.notification;

import java.util.UUID;

public abstract class PostingCommentNotification extends SubscriberNotification {

    private String postingId;
    private String commentId;
    private String commentOwnerName;
    private String commentOwnerFullName;

    public PostingCommentNotification(NotificationType type) {
        super(type);
    }

    public PostingCommentNotification(NotificationType type, UUID postingId, UUID commentId,
                                      String commentOwnerName, String commentOwnerFullName) {
        super(type);
        this.postingId = postingId.toString();
        this.commentId = commentId.toString();
        this.commentOwnerName = commentOwnerName;
        this.commentOwnerFullName = commentOwnerFullName;
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

    public String getCommentOwnerName() {
        return commentOwnerName;
    }

    public void setCommentOwnerName(String commentOwnerName) {
        this.commentOwnerName = commentOwnerName;
    }

    public String getCommentOwnerFullName() {
        return commentOwnerFullName;
    }

    public void setCommentOwnerFullName(String commentOwnerFullName) {
        this.commentOwnerFullName = commentOwnerFullName;
    }

}
