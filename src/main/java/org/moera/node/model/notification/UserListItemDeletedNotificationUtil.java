package org.moera.node.model.notification;

import org.moera.lib.node.types.notifications.UserListItemDeletedNotification;

public class UserListItemDeletedNotificationUtil {
    
    public static UserListItemDeletedNotification build(String listName, String nodeName) {
        UserListItemDeletedNotification notification = new UserListItemDeletedNotification();
        notification.setListName(listName);
        notification.setNodeName(nodeName);
        return notification;
    }

}
