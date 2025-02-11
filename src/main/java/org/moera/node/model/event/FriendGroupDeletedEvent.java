package org.moera.node.model.event;

import java.util.List;

import org.moera.lib.node.types.Scope;
import org.moera.lib.util.LogUtil;
import org.moera.node.auth.principal.PrincipalFilter;
import org.springframework.data.util.Pair;

public class FriendGroupDeletedEvent extends Event {

    private String friendGroupId;

    public FriendGroupDeletedEvent(String friendGroupId, PrincipalFilter filter) {
        super(EventType.FRIEND_GROUP_DELETED, Scope.VIEW_PEOPLE, filter);
        this.friendGroupId = friendGroupId;
    }

    public String getFriendGroupId() {
        return friendGroupId;
    }

    public void setFriendGroupId(String friendGroupId) {
        this.friendGroupId = friendGroupId;
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("id", LogUtil.format(friendGroupId)));
    }

}
