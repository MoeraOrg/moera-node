package org.moera.node.model.notification;

import org.moera.lib.node.types.notifications.UserListItemAddedNotification;

public class UserListItemAddedNotificationUtil {
    
    public static UserListItemAddedNotification build(String listName, String nodeName) {
        UserListItemAddedNotification notification = new UserListItemAddedNotification();
        notification.setListName(listName);
        notification.setNodeName(nodeName);
        return notification;
    }

}
