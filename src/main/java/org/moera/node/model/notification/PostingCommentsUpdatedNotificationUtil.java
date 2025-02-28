package org.moera.node.model.notification;

import java.util.UUID;

import org.moera.lib.node.types.notifications.PostingCommentsUpdatedNotification;

public class PostingCommentsUpdatedNotificationUtil {
    
    public static PostingCommentsUpdatedNotification build(UUID postingId, int total) {
        PostingCommentsUpdatedNotification notification = new PostingCommentsUpdatedNotification();
        notification.setPostingId(postingId.toString());
        notification.setTotal(total);
        return notification;
    }

}
