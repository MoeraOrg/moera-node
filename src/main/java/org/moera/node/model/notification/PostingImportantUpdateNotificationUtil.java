package org.moera.node.model.notification;

import java.util.UUID;

import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.node.types.notifications.PostingImportantUpdateNotification;

public class PostingImportantUpdateNotificationUtil {
    
    public static PostingImportantUpdateNotification build(
        String postingOwnerName,
        String postingOwnerFullName,
        String postingOwnerGender,
        AvatarImage postingOwnerAvatar,
        UUID postingId,
        String postingHeading,
        String description
    ) {
        PostingImportantUpdateNotification notification = new PostingImportantUpdateNotification();
        notification.setPostingOwnerName(postingOwnerName);
        notification.setPostingOwnerFullName(postingOwnerFullName);
        notification.setPostingOwnerGender(postingOwnerGender);
        notification.setPostingOwnerAvatar(postingOwnerAvatar);
        notification.setPostingId(postingId.toString());
        notification.setPostingHeading(postingHeading);
        notification.setDescription(description);
        return notification;
    }

}
