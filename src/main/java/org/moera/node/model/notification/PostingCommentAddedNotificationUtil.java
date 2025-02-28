package org.moera.node.model.notification;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.node.types.SheriffMark;
import org.moera.lib.node.types.notifications.PostingCommentAddedNotification;

public class PostingCommentAddedNotificationUtil {
    
    public static PostingCommentAddedNotification build(
        String postingOwnerName,
        String postingOwnerFullName,
        String postingOwnerGender,
        AvatarImage postingOwnerAvatar,
        UUID postingId,
        String postingHeading,
        List<String> postingSheriffs,
        List<SheriffMark> postingSheriffMarks,
        UUID commentId,
        String commentOwnerName,
        String commentOwnerFullName,
        String commentOwnerGender,
        AvatarImage commentOwnerAvatar,
        String commentHeading,
        List<SheriffMark> commentSheriffMarks,
        UUID commentRepliedTo
    ) {
        PostingCommentAddedNotification notification = new PostingCommentAddedNotification();
        
        notification.setPostingId(postingId.toString());
        notification.setCommentId(commentId.toString());
        notification.setCommentOwnerName(commentOwnerName);
        notification.setCommentOwnerFullName(commentOwnerFullName);
        notification.setCommentOwnerGender(commentOwnerGender);
        notification.setCommentOwnerAvatar(commentOwnerAvatar);
        notification.setPostingOwnerName(postingOwnerName);
        notification.setPostingOwnerFullName(postingOwnerFullName);
        notification.setPostingOwnerGender(postingOwnerGender);
        notification.setPostingOwnerAvatar(postingOwnerAvatar);
        notification.setPostingHeading(postingHeading);
        notification.setPostingSheriffs(postingSheriffs);
        notification.setPostingSheriffMarks(postingSheriffMarks);
        notification.setCommentHeading(commentHeading);
        notification.setCommentSheriffMarks(commentSheriffMarks);
        notification.setCommentRepliedTo(Objects.toString(commentRepliedTo, null));
        
        return notification;
    }

}
