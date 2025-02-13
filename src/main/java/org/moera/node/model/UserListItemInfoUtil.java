package org.moera.node.model;

import org.moera.lib.node.types.UserListItemInfo;
import org.moera.node.data.UserListItem;
import org.moera.node.util.Util;

public class UserListItemInfoUtil {

    public static UserListItemInfo build(UserListItem userListItem) {
        UserListItemInfo info = new UserListItemInfo();
        info.setNodeName(userListItem.getNodeName());
        info.setCreatedAt(Util.toEpochSecond(userListItem.getCreatedAt()));
        info.setMoment(userListItem.getMoment());
        return info;
    }

}
