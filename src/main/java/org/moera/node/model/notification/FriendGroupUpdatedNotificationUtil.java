package org.moera.node.model.notification;

import org.moera.lib.node.types.FriendGroupInfo;
import org.moera.lib.node.types.notifications.FriendGroupUpdatedNotification;

public class FriendGroupUpdatedNotificationUtil {
    
    public static FriendGroupUpdatedNotification build(FriendGroupInfo friendGroup) {
        FriendGroupUpdatedNotification notification = new FriendGroupUpdatedNotification();
        notification.setFriendGroup(friendGroup);
        return notification;
    }

}
