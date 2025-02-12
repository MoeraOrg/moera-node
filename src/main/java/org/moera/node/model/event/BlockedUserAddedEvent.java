package org.moera.node.model.event;

import org.moera.lib.node.types.BlockedUserInfo;
import org.moera.lib.node.types.principal.PrincipalFilter;

public class BlockedUserAddedEvent extends BlockedUserEvent {

    public BlockedUserAddedEvent() {
        super(EventType.BLOCKED_USER_ADDED);
    }

    public BlockedUserAddedEvent(BlockedUserInfo blockedUser, PrincipalFilter filter) {
        super(EventType.BLOCKED_USER_ADDED, blockedUser, filter);
    }
}
