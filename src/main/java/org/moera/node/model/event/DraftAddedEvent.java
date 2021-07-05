package org.moera.node.model.event;

import org.moera.node.data.Draft;

public class DraftAddedEvent extends DraftEvent {

    public DraftAddedEvent() {
        super(EventType.DRAFT_ADDED);
    }

    public DraftAddedEvent(Draft draft) {
        super(EventType.DRAFT_ADDED, draft);
    }

}
