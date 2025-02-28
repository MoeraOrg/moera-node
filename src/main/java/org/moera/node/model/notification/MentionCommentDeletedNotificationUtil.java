package org.moera.node.model.notification;

import java.util.UUID;

import org.moera.lib.node.types.notifications.MentionCommentDeletedNotification;

public class MentionCommentDeletedNotificationUtil {
    
    public static MentionCommentDeletedNotification build(UUID postingId, UUID commentId) {
        MentionCommentDeletedNotification notification = new MentionCommentDeletedNotification();
        notification.setPostingId(postingId.toString());
        notification.setCommentId(commentId.toString());
        return notification;
    }

}
