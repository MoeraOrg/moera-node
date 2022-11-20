package org.moera.node.model.event;

import org.moera.node.auth.principal.PrincipalFilter;
import org.moera.node.model.FriendGroupInfo;

public class FriendGroupAddedEvent extends FriendGroupEvent {

    public FriendGroupAddedEvent(FriendGroupInfo friendGroup, PrincipalFilter filter) {
        super(EventType.FRIEND_GROUP_ADDED, friendGroup, filter);
    }

}
