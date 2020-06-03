package org.moera.node.model.event;

import org.moera.node.data.Posting;

public class PostingAddedEvent extends PostingEvent {

    public PostingAddedEvent() {
        super(EventType.POSTING_ADDED);
    }

    public PostingAddedEvent(Posting posting) {
        super(EventType.POSTING_ADDED, posting);
    }

}
