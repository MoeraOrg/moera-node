package org.moera.node.model.event;

import org.moera.node.data.Posting;

public class PostingRestoredEvent extends PostingEvent {

    public PostingRestoredEvent() {
        super(EventType.POSTING_RESTORED);
    }

    public PostingRestoredEvent(Posting posting) {
        super(EventType.POSTING_RESTORED, posting);
    }

}
