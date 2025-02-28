package org.moera.node.model.notification;

import java.util.UUID;
import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.node.types.notifications.CommentReactionDeletedNotification;

public class CommentReactionDeletedNotificationUtil {
    
    public static CommentReactionDeletedNotification build(
        UUID postingId,
        UUID commentId,
        String ownerName,
        String ownerFullName,
        String ownerGender,
        AvatarImage ownerAvatar,
        boolean negative
    ) {
        CommentReactionDeletedNotification notification = new CommentReactionDeletedNotification();
        notification.setPostingId(postingId.toString());
        notification.setCommentId(commentId.toString());
        notification.setOwnerName(ownerName);
        notification.setOwnerFullName(ownerFullName);
        notification.setOwnerGender(ownerGender);
        notification.setOwnerAvatar(ownerAvatar);
        notification.setNegative(negative);
        return notification;
    }

}
