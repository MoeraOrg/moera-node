package org.moera.node.model.event;

import org.moera.lib.node.types.principal.PrincipalFilter;
import org.moera.node.data.Posting;

public class PostingReactionsChangedEvent extends PostingEvent {

    public PostingReactionsChangedEvent() {
        super(EventType.POSTING_REACTIONS_CHANGED);
    }

    public PostingReactionsChangedEvent(PrincipalFilter filter) {
        super(EventType.POSTING_REACTIONS_CHANGED, filter);
    }

    public PostingReactionsChangedEvent(Posting posting) {
        super(EventType.POSTING_REACTIONS_CHANGED, posting);
    }

    public PostingReactionsChangedEvent(Posting posting, PrincipalFilter filter) {
        super(EventType.POSTING_REACTIONS_CHANGED, posting, filter);
    }

}
