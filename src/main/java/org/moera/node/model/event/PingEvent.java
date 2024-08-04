package org.moera.node.model.event;

import org.moera.node.auth.Scope;

public class PingEvent extends Event {

    public PingEvent() {
        super(EventType.PING, Scope.IDENTIFY);
    }

}
