package org.moera.node.model.event;

import java.util.List;

import org.moera.lib.node.types.Scope;
import org.moera.lib.util.LogUtil;
import org.moera.node.auth.principal.PrincipalFilter;
import org.moera.node.model.FriendGroupInfo;
import org.springframework.data.util.Pair;

public class FriendGroupEvent extends Event {

    private FriendGroupInfo friendGroup;

    public FriendGroupEvent(EventType type, FriendGroupInfo friendGroup) {
        super(type, Scope.VIEW_PEOPLE);
        this.friendGroup = friendGroup;
    }

    public FriendGroupEvent(EventType type, FriendGroupInfo friendGroup, PrincipalFilter filter) {
        super(type, Scope.VIEW_PEOPLE, filter);
        this.friendGroup = friendGroup;
    }

    public FriendGroupInfo getFriendGroup() {
        return friendGroup;
    }

    public void setFriendGroup(FriendGroupInfo friendGroup) {
        this.friendGroup = friendGroup;
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("id", LogUtil.format(friendGroup.getId())));
        parameters.add(Pair.of("title", LogUtil.format(friendGroup.getTitle())));
    }

}
