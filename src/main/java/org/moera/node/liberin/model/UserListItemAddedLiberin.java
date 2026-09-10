package org.moera.node.liberin.model;

import org.moera.node.liberin.Liberin;
import org.moera.node.data.UserListItem;

public class UserListItemAddedLiberin extends Liberin {

    private UserListItem item;

    public UserListItemAddedLiberin(UserListItem item) {
        this.item = item;
    }

    public UserListItem getItem() {
        return item;
    }

    public void setItem(UserListItem item) {
        this.item = item;
    }

}
