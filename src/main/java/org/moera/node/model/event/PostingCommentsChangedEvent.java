package org.moera.node.model.event;

import java.util.List;

import org.moera.lib.node.types.principal.PrincipalFilter;
import org.moera.lib.util.LogUtil;
import org.moera.node.data.Entry;

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
    public void logParameters(List<LogParameter> parameters) {
        super.logParameters(parameters);
        parameters.add(new LogParameter("total", LogUtil.format(total)));
    }

}
