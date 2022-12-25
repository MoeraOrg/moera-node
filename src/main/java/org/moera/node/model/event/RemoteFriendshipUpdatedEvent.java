package org.moera.node.model.event;

import java.util.List;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.principal.PrincipalFilter;
import org.moera.node.event.EventSubscriber;
import org.moera.node.model.FriendOfInfo;
import org.springframework.data.util.Pair;

public class RemoteFriendshipUpdatedEvent extends Event {

    private FriendOfInfo friendOf;

    public RemoteFriendshipUpdatedEvent(FriendOfInfo friendOf, PrincipalFilter filter) {
        super(EventType.REMOTE_FRIENDSHIP_UPDATED, filter);
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
        friendOf.protect(eventSubscriber);
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("nodeName", LogUtil.format(friendOf.getRemoteNodeName())));
    }

}
