package org.moera.node.model.event;

import org.moera.lib.node.types.principal.PrincipalFilter;
import org.moera.node.model.SubscriptionInfo;

public class SubscriptionAddedEvent extends SubscriptionEvent {

    public SubscriptionAddedEvent() {
        super(EventType.SUBSCRIPTION_ADDED);
    }

    public SubscriptionAddedEvent(SubscriptionInfo subscription, PrincipalFilter filter) {
        super(EventType.SUBSCRIPTION_ADDED, subscription, filter);
    }

}
