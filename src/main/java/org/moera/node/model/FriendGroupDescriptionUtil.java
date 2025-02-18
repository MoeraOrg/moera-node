package org.moera.node.model;

import org.moera.lib.node.types.FriendGroupDescription;
import org.moera.lib.node.types.FriendGroupOperations;
import org.moera.node.data.FriendGroup;

public class FriendGroupDescriptionUtil {

    public static void toFriendGroup(FriendGroupDescription description, FriendGroup friendGroup) {
        friendGroup.setTitle(description.getTitle());
        if (FriendGroupOperations.getView(description.getOperations(), null) != null) {
            friendGroup.setViewPrincipal(description.getOperations().getView());
        }
    }

}
