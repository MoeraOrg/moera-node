package org.moera.node.model.event;

import org.moera.node.data.Posting;

public class PostingCommentsChangedEvent extends PostingEvent {

    private int total;

    public PostingCommentsChangedEvent() {
        super(EventType.POSTING_COMMENTS_CHANGED);
    }

    public PostingCommentsChangedEvent(Posting posting) {
        super(EventType.POSTING_COMMENTS_CHANGED, posting);

        total = posting.getChildrenTotal();
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

}
