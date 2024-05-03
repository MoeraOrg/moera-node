package org.moera.node.model.event;

import java.util.List;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.principal.PrincipalFilter;
import org.moera.node.data.Entry;
import org.springframework.data.util.Pair;

public class PostingCommentsChangedEvent extends PostingEvent {

    private int total;

    public PostingCommentsChangedEvent() {
        super(EventType.POSTING_COMMENTS_CHANGED);
    }

    public PostingCommentsChangedEvent(PrincipalFilter filter) {
        super(EventType.POSTING_COMMENTS_CHANGED, filter);
    }

    public PostingCommentsChangedEvent(Entry posting, PrincipalFilter filter) {
        super(EventType.POSTING_COMMENTS_CHANGED, posting, filter);

        total = posting.getTotalChildren();
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("total", LogUtil.format(total)));
    }

}
