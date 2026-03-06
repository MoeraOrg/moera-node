package org.moera.node.model.notification;

import java.util.List;
import java.util.UUID;

import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.node.types.SheriffMark;
import org.moera.lib.node.types.notifications.PremoderatedCommentDecidedNotification;

public class PremoderatedCommentDecidedNotificationUtil {

    public static PremoderatedCommentDecidedNotification build(
        UUID postingId,
        UUID commentId,
        String postingOwnerName,
        String postingOwnerFullName,
        String postingOwnerGender,
        AvatarImage postingOwnerAvatar,
        String postingHeading,
        List<String> postingSheriffs,
        List<SheriffMark> postingSheriffMarks,
        String commentHeading,
        boolean accepted
    ) {
        PremoderatedCommentDecidedNotification notification = new PremoderatedCommentDecidedNotification();

        notification.setPostingId(postingId.toString());
        notification.setCommentId(commentId.toString());
        notification.setPostingOwnerName(postingOwnerName);
        notification.setPostingOwnerFullName(postingOwnerFullName);
        notification.setPostingOwnerGender(postingOwnerGender);
        notification.setPostingOwnerAvatar(postingOwnerAvatar);
        notification.setPostingHeading(postingHeading);
        notification.setPostingSheriffs(postingSheriffs);
        notification.setPostingSheriffMarks(postingSheriffMarks);
        notification.setCommentHeading(commentHeading);
        notification.setAccepted(accepted);

        return notification;
    }

}
