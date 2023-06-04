package org.moera.node.model.event;

import org.moera.node.data.SheriffComplainGroup;

public class SheriffComplainGroupUpdatedEvent extends SheriffComplainGroupEvent {

    public SheriffComplainGroupUpdatedEvent() {
        super(EventType.SHERIFF_COMPLAIN_GROUP_UPDATED);
    }

    public SheriffComplainGroupUpdatedEvent(SheriffComplainGroup group) {
        super(EventType.SHERIFF_COMPLAIN_GROUP_UPDATED, group);
    }

}
