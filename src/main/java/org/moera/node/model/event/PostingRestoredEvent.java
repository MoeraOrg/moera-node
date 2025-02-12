package org.moera.node.model.event;

import org.moera.lib.node.types.principal.PrincipalFilter;
import org.moera.node.data.Posting;

public class PostingRestoredEvent extends PostingEvent {

    public PostingRestoredEvent() {
        super(EventType.POSTING_RESTORED);
    }

    public PostingRestoredEvent(PrincipalFilter filter) {
        super(EventType.POSTING_RESTORED, filter);
    }

    public PostingRestoredEvent(Posting posting) {
        super(EventType.POSTING_RESTORED, posting);
    }

    public PostingRestoredEvent(Posting posting, PrincipalFilter filter) {
        super(EventType.POSTING_RESTORED, posting, filter);
    }

}
