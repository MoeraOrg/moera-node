package org.moera.node.model.event;

import org.moera.lib.node.types.principal.PrincipalFilter;
import org.moera.node.data.Posting;

public class PostingTotalSharingsChangedEvent extends PostingEvent {

    private int total;

    public PostingTotalSharingsChangedEvent() {
        super(EventType.POSTING_TOTAL_SHARINGS_CHANGED);
    }

    public PostingTotalSharingsChangedEvent(Posting posting, int total, PrincipalFilter filter) {
        super(EventType.POSTING_TOTAL_SHARINGS_CHANGED, posting, filter);
        this.total = total;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

}
