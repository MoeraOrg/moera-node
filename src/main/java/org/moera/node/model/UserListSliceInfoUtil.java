package org.moera.node.model;

import org.moera.lib.node.types.UserListSliceInfo;

public class UserListSliceInfoUtil {
    
    public static UserListSliceInfo build(String listName) {
        UserListSliceInfo info = new UserListSliceInfo();
        info.setListName(listName);
        return info;
    }

}
