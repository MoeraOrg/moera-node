package org.moera.node.model.notification;

import java.util.List;
import java.util.UUID;

import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.node.types.SheriffMark;
import org.moera.lib.node.types.notifications.MentionCommentAddedNotification;

public class MentionCommentAddedNotificationUtil {
    
    public static MentionCommentAddedNotification build(
        String postingOwnerName,
        String postingOwnerFullName,
        String postingOwnerGender,
        AvatarImage postingOwnerAvatar,
        UUID postingId,
        UUID commentId,
        String postingHeading,
        List<String> postingSheriffs,
        List<SheriffMark> postingSheriffMarks,
        String commentOwnerName,
        String commentOwnerFullName,
        String commentOwnerGender,
        AvatarImage commentOwnerAvatar,
        String commentHeading,
        List<SheriffMark> commentSheriffMarks
    ) {
        MentionCommentAddedNotification notification = new MentionCommentAddedNotification();
        
        notification.setPostingId(postingId.toString());
        notification.setCommentId(commentId.toString());
        notification.setPostingOwnerName(postingOwnerName);
        notification.setPostingOwnerFullName(postingOwnerFullName);
        notification.setPostingOwnerGender(postingOwnerGender);
        notification.setPostingOwnerAvatar(postingOwnerAvatar);
        notification.setPostingHeading(postingHeading);
        notification.setPostingSheriffs(postingSheriffs);
        notification.setPostingSheriffMarks(postingSheriffMarks);
        notification.setCommentOwnerName(commentOwnerName);
        notification.setCommentOwnerFullName(commentOwnerFullName);
        notification.setCommentOwnerGender(commentOwnerGender);
        notification.setCommentOwnerAvatar(commentOwnerAvatar);
        notification.setCommentHeading(commentHeading);
        notification.setCommentSheriffMarks(commentSheriffMarks);
        
        return notification;
    }

}
