package org.moera.node.model.event;

import org.moera.node.model.BlockedInstantInfo;

public class BlockedInstantDeletedEvent extends BlockedInstantEvent {

    public BlockedInstantDeletedEvent() {
        super(EventType.BLOCKED_INSTANT_DELETED);
    }

    public BlockedInstantDeletedEvent(BlockedInstantInfo blockedInstant) {
        super(EventType.BLOCKED_INSTANT_DELETED, blockedInstant);
    }

}
