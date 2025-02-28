package org.moera.node.model.notification;

import java.util.Collections;
import java.util.List;

import org.moera.lib.node.types.FriendGroupDetails;
import org.moera.lib.node.types.notifications.FriendshipUpdatedNotification;

public class FriendshipUpdatedNotificationUtil {
    
    public static FriendshipUpdatedNotification build(List<FriendGroupDetails> friendGroups) {
        FriendshipUpdatedNotification notification = new FriendshipUpdatedNotification();
        notification.setFriendGroups(friendGroups != null ? friendGroups : Collections.emptyList());
        return notification;
    }

}
