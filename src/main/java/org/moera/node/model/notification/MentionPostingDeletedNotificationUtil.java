package org.moera.node.model.notification;

import java.util.UUID;

import org.moera.lib.node.types.notifications.MentionPostingDeletedNotification;

public class MentionPostingDeletedNotificationUtil {
    
    public static MentionPostingDeletedNotification build(UUID postingId) {
        MentionPostingDeletedNotification notification = new MentionPostingDeletedNotification();
        notification.setPostingId(postingId.toString());
        return notification;
    }

}
