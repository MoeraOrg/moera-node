package org.moera.node.model.event;

import java.util.List;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.principal.PrincipalFilter;
import org.springframework.data.util.Pair;

public class SubscriptionsTotalChangedEvent extends Event {

    private int feedSubscriptionsTotal;

    public SubscriptionsTotalChangedEvent() {
        super(EventType.SUBSCRIPTIONS_TOTAL_CHANGED);
    }

    public SubscriptionsTotalChangedEvent(PrincipalFilter filter) {
        super(EventType.SUBSCRIPTIONS_TOTAL_CHANGED, filter);
    }

    public SubscriptionsTotalChangedEvent(int feedSubscriptionsTotal, PrincipalFilter filter) {
        this(filter);
        this.feedSubscriptionsTotal = feedSubscriptionsTotal;
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
        parameters.add(Pair.of("feedSubscriptionsTotal", LogUtil.format(feedSubscriptionsTotal)));
    }

}
