package org.moera.node.model.notification;

public class UserListItemAddedNotification extends UserListItemNotification {

    public UserListItemAddedNotification() {
        super(NotificationType.USER_LIST_ITEM_ADDED);
    }

    public UserListItemAddedNotification(String listName, String nodeName) {
        super(NotificationType.USER_LIST_ITEM_ADDED, listName, nodeName);
    }

}
