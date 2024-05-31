package org.moera.node.model.event;

import org.moera.node.data.SheriffComplaintGroup;

public class SheriffComplaintGroupAddedEvent extends SheriffComplaintGroupEvent {

    public SheriffComplaintGroupAddedEvent() {
        super(EventType.SHERIFF_COMPLAINT_GROUP_ADDED);
    }

    public SheriffComplaintGroupAddedEvent(SheriffComplaintGroup group) {
        super(EventType.SHERIFF_COMPLAINT_GROUP_ADDED, group);
    }

}
