package org.moera.node.model.event;

import org.moera.node.auth.principal.PrincipalFilter;
import org.moera.node.data.Subscription;

public class SubscriptionDeletedEvent extends SubscriptionEvent {

    public SubscriptionDeletedEvent() {
        super(EventType.SUBSCRIPTION_DELETED);
    }

    public SubscriptionDeletedEvent(Subscription subscription, PrincipalFilter filter) {
        super(EventType.SUBSCRIPTION_DELETED, subscription, filter);
    }

}
