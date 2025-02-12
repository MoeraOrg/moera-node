package org.moera.node.model.event;

import java.util.List;

import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.principal.PrincipalFilter;
import org.moera.lib.util.LogUtil;
import org.springframework.data.util.Pair;

public class SubscriptionsTotalChangedEvent extends Event {

    private int feedSubscriptionsTotal;

    public SubscriptionsTotalChangedEvent() {
        super(EventType.SUBSCRIPTIONS_TOTAL_CHANGED, Scope.VIEW_PEOPLE);
    }

    public SubscriptionsTotalChangedEvent(PrincipalFilter filter) {
        super(EventType.SUBSCRIPTIONS_TOTAL_CHANGED, Scope.VIEW_PEOPLE, filter);
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
