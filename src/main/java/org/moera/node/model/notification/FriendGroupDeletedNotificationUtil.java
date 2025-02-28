package org.moera.node.model.notification;

import java.util.UUID;

import org.moera.lib.node.types.notifications.FriendGroupDeletedNotification;

public class FriendGroupDeletedNotificationUtil {
    
    public static FriendGroupDeletedNotification build(UUID friendGroupId) {
        FriendGroupDeletedNotification notification = new FriendGroupDeletedNotification();
        notification.setFriendGroupId(friendGroupId.toString());
        return notification;
    }

}
