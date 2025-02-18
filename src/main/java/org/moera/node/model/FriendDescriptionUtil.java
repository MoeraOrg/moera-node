package org.moera.node.model;

import org.moera.lib.node.types.FriendDescription;
import org.moera.node.data.Friend;

public class FriendDescriptionUtil {

    public static void toFriend(FriendDescription description, Friend friend) {
        friend.setRemoteNodeName(description.getNodeName());
    }

}
