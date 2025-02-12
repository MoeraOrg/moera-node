package org.moera.node.model.event;

import org.moera.lib.node.types.BlockedByUserInfo;
import org.moera.lib.node.types.principal.PrincipalFilter;

public class BlockedByUserDeletedEvent extends BlockedByUserEvent {

    public BlockedByUserDeletedEvent() {
        super(EventType.BLOCKED_BY_USER_DELETED);
    }

    public BlockedByUserDeletedEvent(BlockedByUserInfo blockedByUser, PrincipalFilter filter) {
        super(EventType.BLOCKED_BY_USER_DELETED, blockedByUser, filter);
    }

}
