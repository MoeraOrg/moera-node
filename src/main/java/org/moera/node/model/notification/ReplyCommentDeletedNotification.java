package org.moera.node.model.notification;

import java.util.UUID;

public class ReplyCommentDeletedNotification extends ReplyCommentNotification {

    private String commentOwnerName;

    public ReplyCommentDeletedNotification() {
        super(NotificationType.REPLY_COMMENT_DELETED);
    }

    public ReplyCommentDeletedNotification(UUID postingId, UUID commentId, UUID repliedToId, String commentOwnerName) {
        super(NotificationType.REPLY_COMMENT_DELETED, postingId, commentId, repliedToId);
        this.commentOwnerName = commentOwnerName;
    }

    public String getCommentOwnerName() {
        return commentOwnerName;
    }

    public void setCommentOwnerName(String commentOwnerName) {
        this.commentOwnerName = commentOwnerName;
    }

}
