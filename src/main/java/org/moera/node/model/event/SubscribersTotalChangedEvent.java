package org.moera.node.model.event;

import java.util.List;

import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.principal.PrincipalFilter;
import org.moera.lib.util.LogUtil;
import org.springframework.data.util.Pair;

public class SubscribersTotalChangedEvent extends Event {

    private int feedSubscribersTotal;

    public SubscribersTotalChangedEvent() {
        super(EventType.SUBSCRIBERS_TOTAL_CHANGED, Scope.VIEW_PEOPLE);
    }

    public SubscribersTotalChangedEvent(PrincipalFilter filter) {
        super(EventType.SUBSCRIBERS_TOTAL_CHANGED, Scope.VIEW_PEOPLE, filter);
    }

    public SubscribersTotalChangedEvent(int feedSubscribersTotal, PrincipalFilter filter) {
        this(filter);
        this.feedSubscribersTotal = feedSubscribersTotal;
    }

    public int getFeedSubscribersTotal() {
        return feedSubscribersTotal;
    }

    public void setFeedSubscribersTotal(int feedSubscribersTotal) {
        this.feedSubscribersTotal = feedSubscribersTotal;
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("feedSubscribersTotal", LogUtil.format(feedSubscribersTotal)));
    }

}
