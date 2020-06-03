package org.moera.node.model.event;

import org.moera.node.data.Posting;

public class PostingDeletedEvent extends PostingEvent {

    public PostingDeletedEvent() {
        super(EventType.POSTING_DELETED);
    }

    public PostingDeletedEvent(Posting posting) {
        super(EventType.POSTING_DELETED, posting);
    }

}
