package org.moera.node.model.event;

import org.moera.lib.node.types.Scope;

public class PingEvent extends Event {

    public PingEvent() {
        super(EventType.PING, Scope.IDENTIFY);
    }

}
