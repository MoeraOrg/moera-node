package org.moera.node.model.event;

import org.moera.node.data.Draft;

public class DraftUpdatedEvent extends DraftEvent {

    public DraftUpdatedEvent() {
        super(EventType.DRAFT_UPDATED);
    }

    public DraftUpdatedEvent(Draft draft) {
        super(EventType.DRAFT_UPDATED, draft);
    }

}
