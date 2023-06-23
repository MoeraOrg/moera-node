package org.moera.node.model.notification;

public class UserListItemDeletedNotification extends UserListItemNotification {

    public UserListItemDeletedNotification() {
        super(NotificationType.USER_LIST_ITEM_DELETED);
    }

    public UserListItemDeletedNotification(String listName, String nodeName) {
        super(NotificationType.USER_LIST_ITEM_DELETED, listName, nodeName);
    }

}
