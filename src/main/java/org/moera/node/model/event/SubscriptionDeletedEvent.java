package org.moera.node.model.event;

import org.moera.node.data.Subscription;

public class SubscriptionDeletedEvent extends SubscriptionEvent {

    public SubscriptionDeletedEvent() {
        super(EventType.SUBSCRIPTION_DELETED);
    }

    public SubscriptionDeletedEvent(Subscription subscription) {
        super(EventType.SUBSCRIPTION_DELETED, subscription);
    }

}
