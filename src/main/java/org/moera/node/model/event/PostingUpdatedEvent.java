package org.moera.node.model.event;

import org.moera.node.auth.principal.PrincipalFilter;
import org.moera.node.data.Posting;

public class PostingUpdatedEvent extends PostingEvent {

    public PostingUpdatedEvent() {
        super(EventType.POSTING_UPDATED);
    }

    public PostingUpdatedEvent(PrincipalFilter filter) {
        super(EventType.POSTING_UPDATED, filter);
    }

    public PostingUpdatedEvent(Posting posting) {
        super(EventType.POSTING_UPDATED, posting);
    }

    public PostingUpdatedEvent(Posting posting, PrincipalFilter filter) {
        super(EventType.POSTING_UPDATED, posting, filter);
    }

}
