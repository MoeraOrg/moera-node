package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.node.data.FriendGroup;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.FriendGroupInfo;

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

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("friendGroup", new FriendGroupInfo(friendGroup, true));
    }

}
