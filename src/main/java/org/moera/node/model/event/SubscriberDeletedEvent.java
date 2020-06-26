package org.moera.node.model.event;

import org.moera.node.data.Subscriber;

public class SubscriberDeletedEvent extends SubscriberEvent {

    public SubscriberDeletedEvent() {
        super(EventType.SUBSCRIBER_DELETED);
    }

    public SubscriberDeletedEvent(Subscriber subscriber) {
        super(EventType.SUBSCRIBER_DELETED, subscriber);
    }

}
