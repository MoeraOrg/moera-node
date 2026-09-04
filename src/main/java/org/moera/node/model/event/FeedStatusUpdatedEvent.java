package org.moera.node.model.event;

import java.util.List;

import org.moera.lib.node.types.FeedStatus;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.util.LogUtil;

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
    public void logParameters(List<LogParameter> parameters) {
        super.logParameters(parameters);
        parameters.add(new LogParameter("feedName", LogUtil.format(feedName)));
        parameters.add(new LogParameter("total", LogUtil.format(status.getTotal())));
        parameters.add(new LogParameter("totalPinned", LogUtil.format(status.getTotalPinned())));
        parameters.add(new LogParameter("notViewed", LogUtil.format(status.getNotViewed())));
        parameters.add(new LogParameter("notRead", LogUtil.format(status.getNotRead())));
    }

}
