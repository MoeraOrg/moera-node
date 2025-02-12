package org.moera.node.model.notification;

import java.util.UUID;

import org.moera.lib.node.types.AvatarImage;

public class PostingCommentDeletedNotification extends PostingCommentNotification {

    public PostingCommentDeletedNotification() {
        super(NotificationType.POSTING_COMMENT_DELETED);
    }

    public PostingCommentDeletedNotification(UUID postingId, UUID commentId, String commentOwnerName,
                                             String commentOwnerFullName, String commentOwnerGender,
                                             AvatarImage commentOwnerAvatar) {
        super(NotificationType.POSTING_COMMENT_DELETED, postingId, commentId, commentOwnerName, commentOwnerFullName,
                commentOwnerGender, commentOwnerAvatar);
    }

}
