package org.moera.node.model.notification;

import java.util.UUID;
import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.node.types.notifications.PostingReactionAddedNotification;

public class PostingReactionAddedNotificationUtil {
    
    public static PostingReactionAddedNotification build(
        String parentPostingNodeName,
        String parentPostingFullName,
        String parentPostingGender,
        AvatarImage parentPostingAvatar,
        UUID parentPostingId,
        UUID parentCommentId,
        UUID parentMediaId,
        String parentHeading,
        UUID postingId,
        String postingHeading,
        String ownerName,
        String ownerFullName,
        String ownerGender,
        AvatarImage ownerAvatar,
        boolean negative,
        int emoji
    ) {
        PostingReactionAddedNotification notification = new PostingReactionAddedNotification();
        
        notification.setParentPostingId(parentPostingId != null ? parentPostingId.toString() : null);
        notification.setParentCommentId(parentCommentId != null ? parentCommentId.toString() : null);
        notification.setParentMediaId(parentMediaId != null ? parentMediaId.toString() : null);
        notification.setPostingId(postingId.toString());
        notification.setOwnerName(ownerName);
        notification.setOwnerFullName(ownerFullName);
        notification.setOwnerGender(ownerGender);
        notification.setOwnerAvatar(ownerAvatar);
        notification.setNegative(negative);
        notification.setParentPostingNodeName(parentPostingNodeName);
        notification.setParentPostingFullName(parentPostingFullName);
        notification.setParentPostingGender(parentPostingGender);
        notification.setParentPostingAvatar(parentPostingAvatar);
        notification.setParentHeading(parentHeading);
        notification.setPostingHeading(postingHeading);
        notification.setEmoji(emoji);
        
        return notification;
    }

}
