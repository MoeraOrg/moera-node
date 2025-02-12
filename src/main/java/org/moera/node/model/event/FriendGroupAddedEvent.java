package org.moera.node.model.event;

import org.moera.lib.node.types.FriendGroupInfo;
import org.moera.lib.node.types.principal.PrincipalFilter;

public class FriendGroupAddedEvent extends FriendGroupEvent {

    public FriendGroupAddedEvent(FriendGroupInfo friendGroup, PrincipalFilter filter) {
        super(EventType.FRIEND_GROUP_ADDED, friendGroup, filter);
    }

}
