package org.moera.node.model.event;

import org.moera.node.data.Posting;

public class DraftPostingUpdatedEvent extends DraftPostingEvent {

    public DraftPostingUpdatedEvent() {
        super(EventType.DRAFT_POSTING_UPDATED);
    }

    public DraftPostingUpdatedEvent(Posting posting) {
        super(EventType.DRAFT_POSTING_UPDATED, posting);
    }

}
