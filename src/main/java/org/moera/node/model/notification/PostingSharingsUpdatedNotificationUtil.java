package org.moera.node.model.notification;

import java.util.UUID;

import org.moera.lib.node.types.notifications.PostingSharingsUpdatedNotification;

public class PostingSharingsUpdatedNotificationUtil {
    
    public static PostingSharingsUpdatedNotification build(UUID postingId, int total) {
        PostingSharingsUpdatedNotification notification = new PostingSharingsUpdatedNotification();
        notification.setPostingId(postingId.toString());
        notification.setTotal(total);
        return notification;
    }

}
