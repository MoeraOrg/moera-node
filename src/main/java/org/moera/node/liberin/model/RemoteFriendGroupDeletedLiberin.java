package org.moera.node.liberin.model;

import org.moera.node.data.FriendOf;
import org.moera.node.liberin.Liberin;

public class RemoteFriendGroupDeletedLiberin extends Liberin {

    private FriendOf friendOf;

    public RemoteFriendGroupDeletedLiberin(FriendOf friendOf) {
        this.friendOf = friendOf;
    }

    public FriendOf getFriendOf() {
        return friendOf;
    }

    public void setFriendOf(FriendOf friendOf) {
        this.friendOf = friendOf;
    }

}
