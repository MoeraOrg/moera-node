package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.node.data.UserListItem;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.UserListItemInfo;

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

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("id", item.getId());
        model.put("listName", item.getListName());
        model.put("item", new UserListItemInfo(item));
    }

}
