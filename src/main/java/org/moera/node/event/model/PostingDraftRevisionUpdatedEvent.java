package org.moera.node.event.model;

import org.moera.node.data.Posting;

public class PostingDraftRevisionUpdatedEvent extends DraftPostingEvent {

    public PostingDraftRevisionUpdatedEvent() {
        super(EventType.POSTING_DRAFT_REVISION_UPDATED);
    }

    public PostingDraftRevisionUpdatedEvent(Posting posting) {
        super(EventType.POSTING_DRAFT_REVISION_UPDATED, posting);
    }

}
