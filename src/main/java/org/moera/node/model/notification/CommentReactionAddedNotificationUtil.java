package org.moera.node.model.notification;

import java.util.UUID;
import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.node.types.notifications.CommentReactionAddedNotification;

public class CommentReactionAddedNotificationUtil {
    
    public static CommentReactionAddedNotification build(
        String postingNodeName,
        String postingFullName,
        String postingGender,
        AvatarImage postingAvatar,
        UUID postingId,
        UUID commentId,
        String postingHeading,
        String commentHeading,
        String ownerName,
        String ownerFullName,
        String ownerGender,
        AvatarImage ownerAvatar,
        boolean negative,
        int emoji
    ) {
        CommentReactionAddedNotification notification = new CommentReactionAddedNotification();
        
        notification.setPostingNodeName(postingNodeName);
        notification.setPostingFullName(postingFullName);
        notification.setPostingGender(postingGender);
        notification.setPostingAvatar(postingAvatar);
        notification.setPostingId(postingId.toString());
        notification.setCommentId(commentId.toString());
        notification.setPostingHeading(postingHeading);
        notification.setCommentHeading(commentHeading);
        notification.setOwnerName(ownerName);
        notification.setOwnerFullName(ownerFullName);
        notification.setOwnerGender(ownerGender);
        notification.setOwnerAvatar(ownerAvatar);
        notification.setNegative(negative);
        notification.setEmoji(emoji);
        
        return notification;
    }

}
