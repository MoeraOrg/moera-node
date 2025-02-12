package org.moera.node.model.notification;

import java.util.UUID;

import org.moera.lib.node.types.AvatarImage;

public class PostingReactionDeletedNotification extends PostingReactionNotification {

    public PostingReactionDeletedNotification() {
        super(NotificationType.POSTING_REACTION_DELETED);
    }

    public PostingReactionDeletedNotification(UUID parentPostingId, UUID parentCommentId, UUID parentMediaId,
                                              UUID postingId, String ownerName, String ownerFullName,
                                              String ownerGender, AvatarImage ownerAvatar, boolean negative) {
        super(NotificationType.POSTING_REACTION_DELETED, parentPostingId, parentCommentId, parentMediaId, postingId,
              ownerName, ownerFullName, ownerGender, ownerAvatar, negative);
    }

}
