package org.moera.node.model.notification;

import java.util.UUID;

import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.node.types.notifications.PostingReactionDeletedNotification;

public class PostingReactionDeletedNotificationUtil {
    
    public static PostingReactionDeletedNotification build(
        UUID parentPostingId,
        UUID parentCommentId,
        UUID parentMediaId,
        UUID postingId,
        String ownerName,
        String ownerFullName,
        String ownerGender,
        AvatarImage ownerAvatar,
        boolean negative
    ) {
        PostingReactionDeletedNotification notification = new PostingReactionDeletedNotification();

        notification.setParentPostingId(parentPostingId != null ? parentPostingId.toString() : null);
        notification.setParentCommentId(parentCommentId != null ? parentCommentId.toString() : null);
        notification.setParentMediaId(parentMediaId != null ? parentMediaId.toString() : null);
        notification.setPostingId(postingId.toString());
        notification.setOwnerName(ownerName);
        notification.setOwnerFullName(ownerFullName);
        notification.setOwnerGender(ownerGender);
        notification.setOwnerAvatar(ownerAvatar);
        notification.setNegative(negative);

        return notification;
    }

}
