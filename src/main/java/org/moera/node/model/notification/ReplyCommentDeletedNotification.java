package org.moera.node.model.notification;

import java.util.UUID;

import org.moera.node.model.AvatarImage;

public class ReplyCommentDeletedNotification extends ReplyCommentNotification {

    public ReplyCommentDeletedNotification() {
        super(NotificationType.REPLY_COMMENT_DELETED);
    }

    public ReplyCommentDeletedNotification(UUID postingId, UUID commentId, UUID repliedToId, String commentOwnerName,
                                           String commentOwnerFullName, AvatarImage commentOwnerAvatar) {
        super(NotificationType.REPLY_COMMENT_DELETED, postingId, commentId, repliedToId, commentOwnerName,
                commentOwnerFullName, commentOwnerAvatar);
    }

}
