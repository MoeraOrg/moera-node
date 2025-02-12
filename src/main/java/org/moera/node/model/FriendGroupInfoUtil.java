package org.moera.node.model;

import org.moera.lib.node.types.FriendGroupInfo;
import org.moera.lib.node.types.FriendGroupOperations;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.FriendGroup;
import org.moera.node.util.Util;

public class FriendGroupInfoUtil {

    public static FriendGroupInfo build(FriendGroup friendGroup, boolean isAdmin) {
        FriendGroupInfo info = new FriendGroupInfo();

        info.setId(friendGroup.getId().toString());
        if (isAdmin || !friendGroup.getViewPrincipal().isAdmin()) {
            info.setTitle(friendGroup.getTitle());
        }
        info.setCreatedAt(Util.toEpochSecond(friendGroup.getCreatedAt()));

        FriendGroupOperations operations = new FriendGroupOperations();
        operations.setView(friendGroup.getViewPrincipal(), Principal.PUBLIC);
        info.setOperations(operations);

        return info;
    }

}
