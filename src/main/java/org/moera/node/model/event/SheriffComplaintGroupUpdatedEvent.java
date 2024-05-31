package org.moera.node.model.event;

import org.moera.node.data.SheriffComplaintGroup;

public class SheriffComplaintGroupUpdatedEvent extends SheriffComplaintGroupEvent {

    public SheriffComplaintGroupUpdatedEvent() {
        super(EventType.SHERIFF_COMPLAINT_GROUP_UPDATED);
    }

    public SheriffComplaintGroupUpdatedEvent(SheriffComplaintGroup group) {
        super(EventType.SHERIFF_COMPLAINT_GROUP_UPDATED, group);
    }

}
