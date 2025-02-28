package org.moera.node.model.notification;

import java.util.UUID;

import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.node.types.notifications.ReplyCommentDeletedNotification;

public class ReplyCommentDeletedNotificationUtil {

    public static ReplyCommentDeletedNotification build(
        UUID postingId,
        UUID commentId,
        UUID repliedToId,
        String commentOwnerName,
        String commentOwnerFullName,
        String commentOwnerGender,
        AvatarImage commentOwnerAvatar
    ) {
        ReplyCommentDeletedNotification notification = new ReplyCommentDeletedNotification();

        notification.setPostingId(postingId.toString());
        notification.setCommentId(commentId.toString());
        notification.setRepliedToId(repliedToId.toString());
        notification.setCommentOwnerName(commentOwnerName);
        notification.setCommentOwnerFullName(commentOwnerFullName);
        notification.setCommentOwnerGender(commentOwnerGender);
        notification.setCommentOwnerAvatar(commentOwnerAvatar);

        return notification;
    }

}
