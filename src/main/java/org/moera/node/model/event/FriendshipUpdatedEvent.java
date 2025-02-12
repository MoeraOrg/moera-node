package org.moera.node.model.event;

import java.util.List;

import org.moera.lib.node.types.FriendInfo;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.util.LogUtil;
import org.moera.node.event.EventSubscriber;
import org.moera.node.model.FriendInfoUtil;
import org.springframework.data.util.Pair;

public class FriendshipUpdatedEvent extends Event {

    private FriendInfo friend;

    public FriendshipUpdatedEvent(FriendInfo friend) {
        super(EventType.FRIENDSHIP_UPDATED, Scope.VIEW_PEOPLE,
                Principal.ADMIN.a().or(Principal.ofNode(friend.getNodeName())));
        this.friend = friend;
    }

    public FriendInfo getFriend() {
        return friend;
    }

    public void setFriend(FriendInfo friend) {
        this.friend = friend;
    }

    @Override
    public void protect(EventSubscriber eventSubscriber) {
        FriendInfoUtil.protect(friend, eventSubscriber);
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("nodeName", LogUtil.format(friend.getNodeName())));
    }

}
