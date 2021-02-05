package org.moera.node.model.notification;

import java.util.UUID;

public class ReplyCommentAddedNotification extends ReplyCommentNotification {

    private String postingHeading;
    private String commentOwnerName;
    private String commentOwnerFullName;
    private String commentHeading;
    private String repliedToHeading;

    public ReplyCommentAddedNotification() {
        super(NotificationType.REPLY_COMMENT_ADDED);
    }

    public ReplyCommentAddedNotification(UUID postingId, UUID commentId, UUID repliedToId, String postingHeading,
                                         String commentOwnerName, String commentOwnerFullName, String commentHeading,
                                         String repliedToHeading) {
        super(NotificationType.REPLY_COMMENT_ADDED, postingId, commentId, repliedToId);
        this.postingHeading = postingHeading;
        this.commentOwnerName = commentOwnerName;
        this.commentOwnerFullName = commentOwnerFullName;
        this.commentHeading = commentHeading;
        this.repliedToHeading = repliedToHeading;
    }

    public String getPostingHeading() {
        return postingHeading;
    }

    public void setPostingHeading(String postingHeading) {
        this.postingHeading = postingHeading;
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

    public String getCommentHeading() {
        return commentHeading;
    }

    public void setCommentHeading(String commentHeading) {
        this.commentHeading = commentHeading;
    }

    public String getRepliedToHeading() {
        return repliedToHeading;
    }

    public void setRepliedToHeading(String repliedToHeading) {
        this.repliedToHeading = repliedToHeading;
    }

}
