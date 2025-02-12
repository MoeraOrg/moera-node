package org.moera.node.model.event;

import org.moera.lib.node.types.principal.PrincipalFilter;
import org.moera.node.data.Posting;

public class PostingAddedEvent extends PostingEvent {

    public PostingAddedEvent() {
        super(EventType.POSTING_ADDED);
    }

    public PostingAddedEvent(PrincipalFilter filter) {
        super(EventType.POSTING_ADDED, filter);
    }

    public PostingAddedEvent(Posting posting) {
        super(EventType.POSTING_ADDED, posting);
    }

    public PostingAddedEvent(Posting posting, PrincipalFilter filter) {
        super(EventType.POSTING_ADDED, posting, filter);
    }

}
