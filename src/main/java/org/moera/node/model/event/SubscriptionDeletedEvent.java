package org.moera.node.model.event;

import org.moera.lib.node.types.SubscriptionInfo;
import org.moera.lib.node.types.principal.PrincipalFilter;

public class SubscriptionDeletedEvent extends SubscriptionEvent {

    public SubscriptionDeletedEvent() {
        super(EventType.SUBSCRIPTION_DELETED);
    }

    public SubscriptionDeletedEvent(SubscriptionInfo subscription, PrincipalFilter filter) {
        super(EventType.SUBSCRIPTION_DELETED, subscription, filter);
    }

}
