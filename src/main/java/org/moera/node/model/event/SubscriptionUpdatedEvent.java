package org.moera.node.model.event;

import org.moera.lib.node.types.principal.PrincipalFilter;
import org.moera.node.model.SubscriptionInfo;

public class SubscriptionUpdatedEvent extends SubscriptionEvent {

    public SubscriptionUpdatedEvent() {
        super(EventType.SUBSCRIPTION_UPDATED);
    }

    public SubscriptionUpdatedEvent(SubscriptionInfo subscription, PrincipalFilter filter) {
        super(EventType.SUBSCRIPTION_UPDATED, subscription, filter);
    }

}
