package org.moera.node.model.event;

import org.moera.node.data.SheriffComplainGroup;

public class SheriffComplainGroupAddedEvent extends SheriffComplainGroupEvent {

    public SheriffComplainGroupAddedEvent() {
        super(EventType.SHERIFF_COMPLAIN_GROUP_ADDED);
    }

    public SheriffComplainGroupAddedEvent(SheriffComplainGroup group) {
        super(EventType.SHERIFF_COMPLAIN_GROUP_ADDED, group);
    }

}
