package org.moera.node.model.event;

import org.moera.lib.node.types.principal.PrincipalFilter;
import org.moera.node.model.BlockedUserInfo;

public class BlockedUserDeletedEvent extends BlockedUserEvent {

    public BlockedUserDeletedEvent() {
        super(EventType.BLOCKED_USER_DELETED);
    }

    public BlockedUserDeletedEvent(BlockedUserInfo blockedUser, PrincipalFilter filter) {
        super(EventType.BLOCKED_USER_DELETED, blockedUser, filter);
    }
}
