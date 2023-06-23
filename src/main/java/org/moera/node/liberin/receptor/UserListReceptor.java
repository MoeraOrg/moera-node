package org.moera.node.liberin.receptor;

import org.moera.node.data.UserListItem;
import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.UserListItemAddedLiberin;
import org.moera.node.liberin.model.UserListItemDeletedLiberin;
import org.moera.node.model.notification.UserListItemAddedNotification;
import org.moera.node.model.notification.UserListItemDeletedNotification;
import org.moera.node.notification.send.Directions;

@LiberinReceptor
public class UserListReceptor extends LiberinReceptorBase {

    @LiberinMapping
    public void added(UserListItemAddedLiberin liberin) {
        UserListItem item = liberin.getItem();
        send(Directions.userListSubscribers(liberin.getNodeId(), item.getListName()),
                new UserListItemAddedNotification(item.getListName(), item.getNodeName()));
    }

    @LiberinMapping
    public void deleted(UserListItemDeletedLiberin liberin) {
        UserListItem item = liberin.getItem();
        send(Directions.userListSubscribers(liberin.getNodeId(), item.getListName()),
                new UserListItemDeletedNotification(item.getListName(), item.getNodeName()));
    }

}
