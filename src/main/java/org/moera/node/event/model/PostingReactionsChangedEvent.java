package org.moera.node.event.model;

import org.moera.node.data.Posting;

public class PostingReactionsChangedEvent extends PostingEvent {

    public PostingReactionsChangedEvent() {
        super(EventType.POSTING_REACTIONS_CHANGED);
    }

    public PostingReactionsChangedEvent(Posting posting) {
        super(EventType.POSTING_REACTIONS_CHANGED, posting);
    }

}
