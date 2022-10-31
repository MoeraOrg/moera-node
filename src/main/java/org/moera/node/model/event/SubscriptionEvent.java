package org.moera.node.model.event;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.commons.util.LogUtil;
import org.moera.node.auth.principal.PrincipalFilter;
import org.moera.node.model.SubscriptionInfo;
import org.springframework.data.util.Pair;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubscriptionEvent extends Event {

    private SubscriptionInfo subscription;

    public SubscriptionEvent(EventType type) {
        super(type);
    }

    public SubscriptionEvent(EventType type, SubscriptionInfo subscription, PrincipalFilter filter) {
        super(type, filter);
        this.subscription = subscription;
    }

    public SubscriptionInfo getSubscription() {
        return subscription;
    }

    public void setSubscription(SubscriptionInfo subscription) {
        this.subscription = subscription;
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("subscriptionType", LogUtil.format(subscription.getType().toString())));
        parameters.add(Pair.of("feedName", LogUtil.format(subscription.getFeedName())));
        parameters.add(Pair.of("remoteNodeName", LogUtil.format(subscription.getRemoteNodeName())));
        parameters.add(Pair.of("remoteFeedName", LogUtil.format(subscription.getRemoteFeedName())));
        parameters.add(Pair.of("remotePostingId", LogUtil.format(subscription.getRemotePostingId())));
    }

}
