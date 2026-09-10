package org.moera.node.liberin.model;

import org.moera.node.data.FriendGroup;
import org.moera.node.liberin.Liberin;

public class FriendGroupAddedLiberin extends Liberin {

    private FriendGroup friendGroup;

    public FriendGroupAddedLiberin(FriendGroup friendGroup) {
        this.friendGroup = friendGroup;
    }

    public FriendGroup getFriendGroup() {
        return friendGroup;
    }

    public void setFriendGroup(FriendGroup friendGroup) {
        this.friendGroup = friendGroup;
    }

}
