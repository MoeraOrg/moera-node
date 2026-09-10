package org.moera.node.liberin.model;

import org.moera.node.liberin.Liberin;
import org.moera.node.data.UserListItem;

public class UserListItemDeletedLiberin extends Liberin {

    private UserListItem item;

    public UserListItemDeletedLiberin(UserListItem item) {
        this.item = item;
    }

    public UserListItem getItem() {
        return item;
    }

    public void setItem(UserListItem item) {
        this.item = item;
    }

}
