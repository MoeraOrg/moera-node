package org.moera.node.model.event;

import org.moera.lib.node.types.SubscriberInfo;
import org.moera.lib.node.types.principal.PrincipalFilter;

public class SubscriberDeletedEvent extends SubscriberEvent {

    public SubscriberDeletedEvent() {
        super(EventType.SUBSCRIBER_DELETED);
    }

    public SubscriberDeletedEvent(SubscriberInfo subscriber, PrincipalFilter filter) {
        super(EventType.SUBSCRIBER_DELETED, subscriber, filter);
    }

}
