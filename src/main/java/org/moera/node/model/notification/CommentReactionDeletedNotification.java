package org.moera.node.model.notification;

import java.util.UUID;

import org.moera.lib.node.types.AvatarImage;

public class CommentReactionDeletedNotification extends CommentReactionNotification {

    public CommentReactionDeletedNotification() {
        super(NotificationType.COMMENT_REACTION_DELETED);
    }

    public CommentReactionDeletedNotification(UUID postingId, UUID commentId, String ownerName, String ownerFullName,
                                              String ownerGender, AvatarImage ownerAvatar, boolean negative) {
        super(NotificationType.COMMENT_REACTION_DELETED, postingId, commentId, ownerName, ownerFullName, ownerGender,
                ownerAvatar, negative);
    }

}
