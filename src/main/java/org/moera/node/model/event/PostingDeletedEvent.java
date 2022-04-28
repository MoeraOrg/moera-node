package org.moera.node.model.event;

import org.moera.node.auth.principal.PrincipalFilter;
import org.moera.node.data.Posting;

public class PostingDeletedEvent extends PostingEvent {

    public PostingDeletedEvent() {
        super(EventType.POSTING_DELETED);
    }

    public PostingDeletedEvent(PrincipalFilter filter) {
        super(EventType.POSTING_DELETED, filter);
    }

    public PostingDeletedEvent(Posting posting) {
        super(EventType.POSTING_DELETED, posting);
    }

    public PostingDeletedEvent(Posting posting, PrincipalFilter filter) {
        super(EventType.POSTING_DELETED, posting, filter);
    }

}
