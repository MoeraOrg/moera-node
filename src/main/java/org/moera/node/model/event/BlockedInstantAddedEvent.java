package org.moera.node.model.event;

import org.moera.node.model.BlockedInstantInfo;

public class BlockedInstantAddedEvent extends BlockedInstantEvent {

    public BlockedInstantAddedEvent() {
        super(EventType.BLOCKED_INSTANT_ADDED);
    }

    public BlockedInstantAddedEvent(BlockedInstantInfo blockedInstant) {
        super(EventType.BLOCKED_INSTANT_ADDED, blockedInstant);
    }

}
