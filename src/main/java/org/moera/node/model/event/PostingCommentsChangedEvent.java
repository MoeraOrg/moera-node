package org.moera.node.model.event;

import org.moera.node.data.Posting;

public class PostingCommentsChangedEvent extends PostingEvent {

    public PostingCommentsChangedEvent() {
        super(EventType.POSTING_COMMENTS_CHANGED);
    }

    public PostingCommentsChangedEvent(Posting posting) {
        super(EventType.POSTING_COMMENTS_CHANGED, posting);
    }

}
