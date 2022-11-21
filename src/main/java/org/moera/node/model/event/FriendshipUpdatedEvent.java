package org.moera.node.model.event;

import java.util.List;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.principal.Principal;
import org.moera.node.model.FriendGroupDetails;
import org.springframework.data.util.Pair;

public class FriendshipUpdatedEvent extends Event {

    private String nodeName;
    private List<FriendGroupDetails> friendGroups;

    public FriendshipUpdatedEvent(String nodeName, List<FriendGroupDetails> friendGroups) {
        super(EventType.FRIENDSHIP_UPDATED, Principal.ADMIN.a().or(Principal.ofNode(nodeName)));
        this.nodeName = nodeName;
        this.friendGroups = friendGroups;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public List<FriendGroupDetails> getFriendGroups() {
        return friendGroups;
    }

    public void setFriendGroups(List<FriendGroupDetails> friendGroups) {
        this.friendGroups = friendGroups;
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("nodeName", LogUtil.format(nodeName)));
    }

}
