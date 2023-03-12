package org.moera.node.model.event;

import org.moera.node.auth.principal.PrincipalFilter;
import org.moera.node.model.BlockedByUserInfo;

public class BlockedByUserAddedEvent extends BlockedByUserEvent {

    public BlockedByUserAddedEvent() {
        super(EventType.BLOCKED_BY_USER_ADDED);
    }

    public BlockedByUserAddedEvent(BlockedByUserInfo blockedByUser, PrincipalFilter filter) {
        super(EventType.BLOCKED_BY_USER_ADDED, blockedByUser, filter);
    }

}
