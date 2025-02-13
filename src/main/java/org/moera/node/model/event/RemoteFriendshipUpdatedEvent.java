package org.moera.node.model.event;

import java.util.List;

import org.moera.lib.node.types.FriendOfInfo;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.principal.PrincipalFilter;
import org.moera.lib.util.LogUtil;
import org.moera.node.event.EventSubscriber;
import org.moera.node.model.FriendOfInfoUtil;
import org.springframework.data.util.Pair;

public class RemoteFriendshipUpdatedEvent extends Event {

    private FriendOfInfo friendOf;

    public RemoteFriendshipUpdatedEvent(FriendOfInfo friendOf, PrincipalFilter filter) {
        super(EventType.REMOTE_FRIENDSHIP_UPDATED, Scope.VIEW_PEOPLE, filter);
        this.friendOf = friendOf;
    }

    public FriendOfInfo getFriendOf() {
        return friendOf;
    }

    public void setFriendOf(FriendOfInfo friendOf) {
        this.friendOf = friendOf;
    }

    @Override
    public void protect(EventSubscriber eventSubscriber) {
        FriendOfInfoUtil.protect(friendOf, eventSubscriber);
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("nodeName", LogUtil.format(friendOf.getRemoteNodeName())));
    }

}
