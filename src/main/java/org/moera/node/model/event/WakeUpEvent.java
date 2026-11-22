package org.moera.node.model.event;

import org.moera.lib.node.types.Scope;

public class WakeUpEvent extends Event {

    public WakeUpEvent() {
        super(EventType.WAKE_UP, Scope.IDENTIFY);
    }

}
