package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.node.data.FriendOf;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.FriendOfInfoUtil;

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

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("friendOf", FriendOfInfoUtil.build(friendOf, getPluginContext().getOptions()));
    }

}
