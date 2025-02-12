package org.moera.node.model.event;

import org.moera.lib.node.types.BlockedUserInfo;
import org.moera.lib.node.types.principal.PrincipalFilter;

public class BlockedUserDeletedEvent extends BlockedUserEvent {

    public BlockedUserDeletedEvent() {
        super(EventType.BLOCKED_USER_DELETED);
    }

    public BlockedUserDeletedEvent(BlockedUserInfo blockedUser, PrincipalFilter filter) {
        super(EventType.BLOCKED_USER_DELETED, blockedUser, filter);
    }
}
