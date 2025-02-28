package org.moera.node.model.notification;

import java.util.UUID;

import org.moera.lib.node.types.notifications.PostingUpdatedNotification;

public class PostingUpdatedNotificationUtil {
    
    public static PostingUpdatedNotification build(UUID postingId) {
        PostingUpdatedNotification notification = new PostingUpdatedNotification();
        notification.setPostingId(postingId.toString());
        return notification;
    }

}
