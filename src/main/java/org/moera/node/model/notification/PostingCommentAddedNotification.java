package org.moera.node.model.notification;

import java.util.UUID;

public class PostingCommentAddedNotification extends PostingCommentNotification {

    private String postingHeading;
    private String commentHeading;

    public PostingCommentAddedNotification() {
        super(NotificationType.POSTING_COMMENT_ADDED);
    }

    public PostingCommentAddedNotification(UUID postingId, String postingHeading, UUID commentId,
                                           String commentOwnerName, String commentHeading) {
        super(NotificationType.POSTING_COMMENT_ADDED, postingId, commentId, commentOwnerName);
        this.postingHeading = postingHeading;
        this.commentHeading = commentHeading;
    }

    public String getPostingHeading() {
        return postingHeading;
    }

    public void setPostingHeading(String postingHeading) {
        this.postingHeading = postingHeading;
    }

    public String getCommentHeading() {
        return commentHeading;
    }

    public void setCommentHeading(String commentHeading) {
        this.commentHeading = commentHeading;
    }

}
