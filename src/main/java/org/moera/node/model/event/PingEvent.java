package org.moera.node.model.event;

import org.moera.node.auth.principal.Principal;

public class PingEvent extends Event {

    public PingEvent() {
        super(EventType.PING, Principal.ADMIN);
    }

}
