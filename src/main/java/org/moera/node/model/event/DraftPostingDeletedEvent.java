package org.moera.node.model.event;

import org.moera.node.data.Posting;

public class DraftPostingDeletedEvent extends DraftPostingEvent {

    public DraftPostingDeletedEvent() {
        super(EventType.DRAFT_POSTING_DELETED);
    }

    public DraftPostingDeletedEvent(Posting posting) {
        super(EventType.DRAFT_POSTING_DELETED, posting);
    }

}
