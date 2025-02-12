package org.moera.node.model.event;

import org.moera.lib.node.types.SubscriberInfo;
import org.moera.lib.node.types.principal.PrincipalFilter;

public class SubscriberAddedEvent extends SubscriberEvent {

    public SubscriberAddedEvent() {
        super(EventType.SUBSCRIBER_ADDED);
    }

    public SubscriberAddedEvent(SubscriberInfo subscriber, PrincipalFilter filter) {
        super(EventType.SUBSCRIBER_ADDED, subscriber, filter);
    }

}
