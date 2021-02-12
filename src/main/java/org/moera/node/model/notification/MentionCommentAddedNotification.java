package org.moera.node.model.notification;

import java.util.UUID;
import javax.validation.constraints.Size;

public class MentionCommentAddedNotification extends MentionCommentNotification {

    private String postingHeading;

    @Size(max = 63)
    private String commentOwnerName;

    @Size(max = 96)
    private String commentOwnerFullName;

    private String commentHeading;

    public MentionCommentAddedNotification() {
        super(NotificationType.MENTION_COMMENT_ADDED);
    }

    public MentionCommentAddedNotification(UUID postingId, UUID commentId, String postingHeading,
                                           String commentOwnerName, String commentOwnerFullName,
                                           String commentHeading) {
        super(NotificationType.MENTION_COMMENT_ADDED, postingId, commentId);
        this.postingHeading = postingHeading;
        this.commentOwnerName = commentOwnerName;
        this.commentOwnerFullName = commentOwnerFullName;
        this.commentHeading = commentHeading;
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

}
