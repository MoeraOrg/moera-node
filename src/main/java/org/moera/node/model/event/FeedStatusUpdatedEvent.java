package org.moera.node.model.event;

import java.util.List;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.Scope;
import org.moera.node.auth.principal.Principal;
import org.moera.node.model.FeedStatus;
import org.springframework.data.util.Pair;

public class FeedStatusUpdatedEvent extends Event {

    private String feedName;
    private FeedStatus status;

    public FeedStatusUpdatedEvent() {
        super(EventType.FEED_STATUS_UPDATED, Scope.VIEW_FEEDS);
    }

    public FeedStatusUpdatedEvent(String feedName, FeedStatus status, boolean isAdmin) {
        super(EventType.FEED_STATUS_UPDATED, Scope.VIEW_FEEDS, isAdmin ? Principal.ADMIN : Principal.PUBLIC);

        this.feedName = feedName;
        this.status = status;
    }

    public String getFeedName() {
        return feedName;
    }

    public void setFeedName(String feedName) {
        this.feedName = feedName;
    }

    public FeedStatus getStatus() {
        return status;
    }

    public void setStatus(FeedStatus status) {
        this.status = status;
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("feedName", LogUtil.format(feedName)));
        parameters.add(Pair.of("total", LogUtil.format(status.getTotal())));
        parameters.add(Pair.of("totalPinned", LogUtil.format(status.getTotalPinned())));
        parameters.add(Pair.of("notViewed", LogUtil.format(status.getNotViewed())));
        parameters.add(Pair.of("notRead", LogUtil.format(status.getNotRead())));
    }

}
