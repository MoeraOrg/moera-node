package org.moera.node.model.notification;

import java.util.UUID;

import org.moera.lib.node.types.notifications.PostingDeletedNotification;

public class PostingDeletedNotificationUtil {
    
    public static PostingDeletedNotification build(UUID postingId) {
        PostingDeletedNotification notification = new PostingDeletedNotification();
        notification.setPostingId(postingId.toString());
        return notification;
    }

}
