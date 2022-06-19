package org.moera.node.model.event;

import org.moera.node.auth.principal.PrincipalFilter;
import org.moera.node.data.Subscriber;

public class SubscriberAddedEvent extends SubscriberEvent {

    public SubscriberAddedEvent() {
        super(EventType.SUBSCRIBER_ADDED);
    }

    public SubscriberAddedEvent(Subscriber subscriber, PrincipalFilter filter) {
        super(EventType.SUBSCRIBER_ADDED, subscriber, filter);
    }

}
