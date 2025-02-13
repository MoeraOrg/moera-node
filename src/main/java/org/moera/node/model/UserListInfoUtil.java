package org.moera.node.model;

import org.moera.lib.node.types.UserListInfo;

public class UserListInfoUtil {
    
    public static UserListInfo build(String name, int total) {
        UserListInfo userListInfo = new UserListInfo();
        userListInfo.setName(name);
        userListInfo.setTotal(total);
        return userListInfo;
    }

}
