package org.moera.node.model.notification;

import java.util.UUID;

import org.moera.node.model.AvatarImage;

public class CommentReactionDeletedNotification extends CommentReactionNotification {

    public CommentReactionDeletedNotification() {
        super(NotificationType.COMMENT_REACTION_DELETED);
    }

    public CommentReactionDeletedNotification(UUID postingId, UUID commentId, String ownerName, String ownerFullName,
                                              AvatarImage ownerAvatar, boolean negative) {
        super(NotificationType.COMMENT_REACTION_DELETED, postingId, commentId, ownerName, ownerFullName, ownerAvatar,
                negative);
    }

}
