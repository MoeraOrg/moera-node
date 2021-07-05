package org.moera.node.model.event;

import org.moera.node.data.Draft;

public class DraftDeletedEvent extends DraftEvent {

    public DraftDeletedEvent() {
        super(EventType.DRAFT_DELETED);
    }

    public DraftDeletedEvent(Draft draft) {
        super(EventType.DRAFT_DELETED, draft);
    }

}
