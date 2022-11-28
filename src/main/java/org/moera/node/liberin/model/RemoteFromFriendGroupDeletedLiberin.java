package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.node.data.FriendOf;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.FriendOfInfo;

public class RemoteFromFriendGroupDeletedLiberin extends Liberin {

    private FriendOf friendOf;

    public RemoteFromFriendGroupDeletedLiberin(FriendOf friendOf) {
        this.friendOf = friendOf;
    }

    public FriendOf getFriendOf() {
        return friendOf;
    }

    public void setFriendOf(FriendOf friendOf) {
        this.friendOf = friendOf;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("friendOf", new FriendOfInfo(friendOf));
    }

}
