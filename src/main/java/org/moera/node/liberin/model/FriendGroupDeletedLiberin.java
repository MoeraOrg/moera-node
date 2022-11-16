package org.moera.node.liberin.model;

import java.util.Map;
import java.util.UUID;

import org.moera.node.liberin.Liberin;

public class FriendGroupDeletedLiberin extends Liberin {

    private UUID friendGroupId;

    public FriendGroupDeletedLiberin(UUID friendGroupId) {
        this.friendGroupId = friendGroupId;
    }

    public UUID getFriendGroupId() {
        return friendGroupId;
    }

    public void setFriendGroupId(UUID friendGroupId) {
        this.friendGroupId = friendGroupId;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("friendGroupId", friendGroupId);
    }

}
