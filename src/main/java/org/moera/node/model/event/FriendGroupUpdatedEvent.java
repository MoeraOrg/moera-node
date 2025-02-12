package org.moera.node.model.event;

import org.moera.lib.node.types.FriendGroupInfo;
import org.moera.lib.node.types.principal.PrincipalFilter;

public class FriendGroupUpdatedEvent extends FriendGroupEvent {

    public FriendGroupUpdatedEvent(FriendGroupInfo friendGroup, PrincipalFilter filter) {
        super(EventType.FRIEND_GROUP_UPDATED, friendGroup, filter);
    }

}
