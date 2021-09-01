package org.moera.node.model.event;

import java.util.List;

import org.moera.commons.util.LogUtil;
import org.moera.node.event.EventSubscriber;
import org.moera.node.model.FeedStatus;
import org.springframework.data.util.Pair;

public class FeedStatusUpdatedEvent extends Event {

    private String feedName;
    private int total;
    private int totalPinned;
    private int notViewed;
    private int notRead;

    public FeedStatusUpdatedEvent() {
        super(EventType.FEED_STATUS_UPDATED);
    }

    public FeedStatusUpdatedEvent(String feedName, FeedStatus feedStatus) {
        super(EventType.FEED_STATUS_UPDATED);

        this.feedName = feedName;
        total = feedStatus.getTotal();
        totalPinned = feedStatus.getTotalPinned();
        notViewed = feedStatus.getNotViewed();
        notRead = feedStatus.getNotRead();
    }

    public String getFeedName() {
        return feedName;
    }

    public void setFeedName(String feedName) {
        this.feedName = feedName;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getTotalPinned() {
        return totalPinned;
    }

    public void setTotalPinned(int totalPinned) {
        this.totalPinned = totalPinned;
    }

    public int getNotViewed() {
        return notViewed;
    }

    public void setNotViewed(int notViewed) {
        this.notViewed = notViewed;
    }

    public int getNotRead() {
        return notRead;
    }

    public void setNotRead(int notRead) {
        this.notRead = notRead;
    }

    @Override
    public boolean isPermitted(EventSubscriber subscriber) {
        return subscriber.isAdmin();
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("feedName", LogUtil.format(feedName)));
        parameters.add(Pair.of("notViewed", LogUtil.format(notViewed)));
        parameters.add(Pair.of("notRead", LogUtil.format(notRead)));
    }

}
