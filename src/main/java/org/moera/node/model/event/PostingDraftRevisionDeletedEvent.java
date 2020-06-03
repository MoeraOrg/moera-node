package org.moera.node.model.event;

import org.moera.node.data.Posting;

public class PostingDraftRevisionDeletedEvent extends DraftPostingEvent {

    public PostingDraftRevisionDeletedEvent() {
        super(EventType.POSTING_DRAFT_REVISION_DELETED);
    }

    public PostingDraftRevisionDeletedEvent(Posting posting) {
        super(EventType.POSTING_DRAFT_REVISION_DELETED, posting);
    }

}
