package org.moera.node.model.notification;

import java.util.UUID;

import org.moera.lib.node.types.notifications.PostingReactionDeletedAllNotification;

public class PostingReactionDeletedAllNotificationUtil {
    
    public static PostingReactionDeletedAllNotification build(
        UUID parentPostingId,
        UUID parentCommentId,
        UUID parentMediaId,
        UUID postingId
    ) {
        PostingReactionDeletedAllNotification notification = new PostingReactionDeletedAllNotification();
        notification.setParentPostingId(parentPostingId != null ? parentPostingId.toString() : null);
        notification.setParentCommentId(parentCommentId != null ? parentCommentId.toString() : null);
        notification.setParentMediaId(parentMediaId != null ? parentMediaId.toString() : null);
        notification.setPostingId(postingId.toString());
        return notification;
    }

}
