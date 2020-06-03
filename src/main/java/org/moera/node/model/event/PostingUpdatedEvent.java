package org.moera.node.model.event;

import org.moera.node.data.Posting;

public class PostingUpdatedEvent extends PostingEvent {

    public PostingUpdatedEvent() {
        super(EventType.POSTING_UPDATED);
    }

    public PostingUpdatedEvent(Posting posting) {
        super(EventType.POSTING_UPDATED, posting);
    }

}
