package org.moera.node.model.event;

import org.moera.node.auth.principal.PrincipalFilter;
import org.moera.node.model.BlockedByUserInfo;

public class BlockedByUserDeletedEvent extends BlockedByUserEvent {

    public BlockedByUserDeletedEvent() {
        super(EventType.BLOCKED_BY_USER_DELETED);
    }

    public BlockedByUserDeletedEvent(BlockedByUserInfo blockedByUser, PrincipalFilter filter) {
        super(EventType.BLOCKED_BY_USER_DELETED, blockedByUser, filter);
    }

}
