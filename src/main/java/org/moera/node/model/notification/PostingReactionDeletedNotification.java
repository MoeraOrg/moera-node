package org.moera.node.model.notification;

import java.util.UUID;

import org.moera.node.model.AvatarImage;

public class PostingReactionDeletedNotification extends PostingReactionNotification {

    public PostingReactionDeletedNotification() {
        super(NotificationType.POSTING_REACTION_DELETED);
    }

    public PostingReactionDeletedNotification(UUID parentPostingId, UUID parentCommentId, UUID parentMediaId,
                                              UUID postingId, String ownerName, String ownerFullName,
                                              AvatarImage ownerAvatar, boolean negative) {
        super(NotificationType.POSTING_REACTION_DELETED, parentPostingId, parentCommentId, parentMediaId, postingId,
              ownerName, ownerFullName, ownerAvatar, negative);
    }

}
