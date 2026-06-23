package org.moera.node.model.event;

import java.util.List;
import java.util.UUID;

import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.util.LogUtil;
import org.springframework.data.util.Pair;

public class PostingViewedEvent extends PostingEvent {

    private int viewCount;

    public PostingViewedEvent() {
        super(EventType.POSTING_VIEWED, Principal.ADMIN);
    }

    public PostingViewedEvent(UUID postingId, int viewCount) {
        super(EventType.POSTING_VIEWED, postingId, Principal.ADMIN);
        this.viewCount = viewCount;
    }

    public int getViewCount() {
        return viewCount;
    }

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("viewCount", LogUtil.format(viewCount)));
    }

}
