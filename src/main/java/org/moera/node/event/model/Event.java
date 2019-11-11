package org.moera.node.event.model;

import org.moera.node.event.EventSubscriber;

public abstract class Event {

    private EventType type;

    protected Event(EventType type) {
        this.type = type;
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public boolean isPermitted(EventSubscriber subscriber) {
        return true;
    }

}
