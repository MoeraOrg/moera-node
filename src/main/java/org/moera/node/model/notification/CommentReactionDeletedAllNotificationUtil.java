package org.moera.node.model.notification;

import java.util.UUID;

import org.moera.lib.node.types.notifications.CommentReactionDeletedAllNotification;

public class CommentReactionDeletedAllNotificationUtil {
    
    public static CommentReactionDeletedAllNotification build(UUID postingId, UUID commentId) {
        CommentReactionDeletedAllNotification notification = new CommentReactionDeletedAllNotification();
        notification.setPostingId(postingId.toString());
        notification.setCommentId(commentId.toString());
        return notification;
    }

}
