package org.moera.node.model.event;

import org.moera.node.auth.principal.PrincipalFilter;
import org.moera.node.model.FriendGroupInfo;

public class FriendGroupUpdatedEvent extends FriendGroupEvent {

    public FriendGroupUpdatedEvent(FriendGroupInfo friendGroup, PrincipalFilter filter) {
        super(EventType.FRIEND_GROUP_UPDATED, friendGroup, filter);
    }

}
