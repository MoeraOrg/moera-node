package org.moera.node.model.event;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.SubscriptionInfo;
import org.moera.lib.node.types.principal.PrincipalFilter;
import org.moera.lib.util.LogUtil;
import org.moera.node.event.EventSubscriber;
import org.moera.node.model.SubscriptionInfoUtil;
import org.springframework.data.util.Pair;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubscriptionEvent extends Event {

    private SubscriptionInfo subscription;

    public SubscriptionEvent(EventType type) {
        super(type, Scope.VIEW_PEOPLE);
    }

    public SubscriptionEvent(EventType type, SubscriptionInfo subscription, PrincipalFilter filter) {
        super(type, Scope.VIEW_PEOPLE, filter);
        this.subscription = subscription;
    }

    public SubscriptionInfo getSubscription() {
        return subscription;
    }

    public void setSubscription(SubscriptionInfo subscription) {
        this.subscription = subscription;
    }

    @Override
    public void protect(EventSubscriber eventSubscriber) {
        SubscriptionInfoUtil.protect(subscription, eventSubscriber);
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
