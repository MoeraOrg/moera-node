package org.moera.node.model.event;

import org.moera.node.data.Posting;

public class DraftPostingAddedEvent extends DraftPostingEvent {

    public DraftPostingAddedEvent() {
        super(EventType.DRAFT_POSTING_ADDED);
    }

    public DraftPostingAddedEvent(Posting posting) {
        super(EventType.DRAFT_POSTING_ADDED, posting);
    }

}
