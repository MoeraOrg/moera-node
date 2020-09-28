package org.moera.node.model.notification;

import java.util.UUID;

public class PostingCommentAddedNotification extends Notification {

    private String postingId;
    private String postingHeading;
    private String commentId;
    private String commentOwnerName;
    private String commentHeading;

    public PostingCommentAddedNotification() {
        super(NotificationType.POSTING_COMMENT_ADDED);
    }

    public PostingCommentAddedNotification(UUID postingId, String postingHeading, UUID commentId,
                                           String commentOwnerName, String commentHeading) {
        super(NotificationType.POSTING_COMMENT_ADDED);
        this.postingId = postingId.toString();
        this.postingHeading = postingHeading;
        this.commentId = commentId.toString();
        this.commentOwnerName = commentOwnerName;
        this.commentHeading = commentHeading;
    }

    public String getPostingId() {
        return postingId;
    }

    public void setPostingId(String postingId) {
        this.postingId = postingId;
    }

    public String getPostingHeading() {
        return postingHeading;
    }

    public void setPostingHeading(String postingHeading) {
        this.postingHeading = postingHeading;
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

    public String getCommentHeading() {
        return commentHeading;
    }

    public void setCommentHeading(String commentHeading) {
        this.commentHeading = commentHeading;
    }

}
