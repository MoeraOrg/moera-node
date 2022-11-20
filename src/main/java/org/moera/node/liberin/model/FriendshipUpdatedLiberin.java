package org.moera.node.liberin.model;

import java.util.List;
import java.util.Map;

import org.moera.node.liberin.Liberin;
import org.moera.node.model.FriendGroupDetails;

public class FriendshipUpdatedLiberin extends Liberin {

    private String friendNodeName;
    private List<FriendGroupDetails> friendGroups;

    public FriendshipUpdatedLiberin(String friendNodeName, List<FriendGroupDetails> friendGroups) {
        this.friendNodeName = friendNodeName;
        this.friendGroups = friendGroups;
    }

    public String getFriendNodeName() {
        return friendNodeName;
    }

    public void setFriendNodeName(String friendNodeName) {
        this.friendNodeName = friendNodeName;
    }

    public List<FriendGroupDetails> getFriendGroups() {
        return friendGroups;
    }

    public void setFriendGroups(List<FriendGroupDetails> friendGroups) {
        this.friendGroups = friendGroups;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("friendNodeName", friendNodeName);
        model.put("friendGroups", friendGroups);
    }

}
