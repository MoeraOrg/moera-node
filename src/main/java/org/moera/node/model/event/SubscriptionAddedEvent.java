package org.moera.node.model.event;

import org.moera.node.auth.principal.PrincipalFilter;
import org.moera.node.data.Subscription;

public class SubscriptionAddedEvent extends SubscriptionEvent {

    public SubscriptionAddedEvent() {
        super(EventType.SUBSCRIPTION_ADDED);
    }

    public SubscriptionAddedEvent(Subscription subscription, PrincipalFilter filter) {
        super(EventType.SUBSCRIPTION_ADDED, subscription, filter);
    }

}
