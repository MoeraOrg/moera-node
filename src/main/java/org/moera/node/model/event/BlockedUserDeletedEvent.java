package org.moera.node.model.event;

import org.moera.node.model.BlockedUserInfo;

public class BlockedUserDeletedEvent extends BlockedUserEvent {

    public BlockedUserDeletedEvent() {
        super(EventType.BLOCKED_USER_DELETED);
    }

    public BlockedUserDeletedEvent(BlockedUserInfo blockedUser) {
        super(EventType.BLOCKED_USER_DELETED, blockedUser);
    }
}
