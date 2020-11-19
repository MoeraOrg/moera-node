package org.moera.node.model.event;

import org.moera.node.event.EventSubscriber;

public class PingEvent extends Event {

    public PingEvent() {
        super(EventType.PING);
    }

    @Override
    public boolean isPermitted(EventSubscriber subscriber) {
        return subscriber.isAdmin();
    }

}
