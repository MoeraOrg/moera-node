package org.moera.node.model.event;

import org.moera.lib.node.types.principal.PrincipalFilter;
import org.moera.node.model.SubscriberInfo;

public class SubscriberUpdatedEvent extends SubscriberEvent {

    public SubscriberUpdatedEvent() {
        super(EventType.SUBSCRIBER_UPDATED);
    }

    public SubscriberUpdatedEvent(SubscriberInfo subscriber, PrincipalFilter filter) {
        super(EventType.SUBSCRIBER_UPDATED, subscriber, filter);
    }

}
