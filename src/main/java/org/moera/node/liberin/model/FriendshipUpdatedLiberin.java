package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.node.liberin.Liberin;

public class FriendshipUpdatedLiberin extends Liberin {

    private String friendNodeName;

    public FriendshipUpdatedLiberin(String friendNodeName) {
        this.friendNodeName = friendNodeName;
    }

    public String getFriendNodeName() {
        return friendNodeName;
    }

    public void setFriendNodeName(String friendNodeName) {
        this.friendNodeName = friendNodeName;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("friendNodeName", friendNodeName);
    }

}
