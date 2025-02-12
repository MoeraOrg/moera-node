package org.moera.node.model.event;

import org.moera.lib.node.types.principal.PrincipalFilter;
import org.moera.node.model.SubscriptionInfo;

public class SubscriptionDeletedEvent extends SubscriptionEvent {

    public SubscriptionDeletedEvent() {
        super(EventType.SUBSCRIPTION_DELETED);
    }

    public SubscriptionDeletedEvent(SubscriptionInfo subscription, PrincipalFilter filter) {
        super(EventType.SUBSCRIPTION_DELETED, subscription, filter);
    }

}
