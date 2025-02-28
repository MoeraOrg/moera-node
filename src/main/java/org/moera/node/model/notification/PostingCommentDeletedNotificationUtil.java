package org.moera.node.model.notification;

import java.util.UUID;
import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.node.types.notifications.PostingCommentDeletedNotification;

public class PostingCommentDeletedNotificationUtil {
    
    public static PostingCommentDeletedNotification build(
        UUID postingId,
        UUID commentId,
        String commentOwnerName,
        String commentOwnerFullName,
        String commentOwnerGender,
        AvatarImage commentOwnerAvatar
    ) {
        PostingCommentDeletedNotification notification = new PostingCommentDeletedNotification();
        notification.setPostingId(postingId.toString());
        notification.setCommentId(commentId.toString());
        notification.setCommentOwnerName(commentOwnerName);
        notification.setCommentOwnerFullName(commentOwnerFullName);
        notification.setCommentOwnerGender(commentOwnerGender);
        notification.setCommentOwnerAvatar(commentOwnerAvatar);
        return notification;
    }

}
