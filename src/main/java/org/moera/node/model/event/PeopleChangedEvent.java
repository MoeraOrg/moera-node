package org.moera.node.model.event;

import java.util.List;

import org.moera.commons.util.LogUtil;
import org.springframework.data.util.Pair;

public class PeopleChangedEvent extends Event {

    private int feedSubscribersTotal;
    private int feedSubscriptionsTotal;

    public PeopleChangedEvent() {
        super(EventType.PEOPLE_CHANGED);
    }

    public PeopleChangedEvent(int feedSubscribersTotal, int feedSubscriptionsTotal) {
        this();
        this.feedSubscribersTotal = feedSubscribersTotal;
        this.feedSubscriptionsTotal = feedSubscriptionsTotal;
    }

    public int getFeedSubscribersTotal() {
        return feedSubscribersTotal;
    }

    public void setFeedSubscribersTotal(int feedSubscribersTotal) {
        this.feedSubscribersTotal = feedSubscribersTotal;
    }

    public int getFeedSubscriptionsTotal() {
        return feedSubscriptionsTotal;
    }

    public void setFeedSubscriptionsTotal(int feedSubscriptionsTotal) {
        this.feedSubscriptionsTotal = feedSubscriptionsTotal;
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("feedSubscribersTotal", LogUtil.format(feedSubscribersTotal)));
        parameters.add(Pair.of("feedSubscriptionsTotal", LogUtil.format(feedSubscriptionsTotal)));
    }

}
