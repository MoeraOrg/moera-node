package org.moera.node.model.event;

import org.moera.lib.node.types.BlockedByUserInfo;
import org.moera.lib.node.types.principal.PrincipalFilter;

public class BlockedByUserAddedEvent extends BlockedByUserEvent {

    public BlockedByUserAddedEvent() {
        super(EventType.BLOCKED_BY_USER_ADDED);
    }

    public BlockedByUserAddedEvent(BlockedByUserInfo blockedByUser, PrincipalFilter filter) {
        super(EventType.BLOCKED_BY_USER_ADDED, blockedByUser, filter);
    }

}
