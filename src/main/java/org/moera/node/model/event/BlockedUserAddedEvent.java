package org.moera.node.model.event;

import org.moera.node.model.BlockedUserInfo;

public class BlockedUserAddedEvent extends BlockedUserEvent {

    public BlockedUserAddedEvent() {
        super(EventType.BLOCKED_USER_ADDED);
    }

    public BlockedUserAddedEvent(BlockedUserInfo blockedUser) {
        super(EventType.BLOCKED_USER_ADDED, blockedUser);
    }
}
