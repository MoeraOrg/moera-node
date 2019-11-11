package org.moera.node.event.model;

import org.moera.node.event.EventSubscriber;

public class NodeSettingsChangedEvent extends Event {

    public NodeSettingsChangedEvent() {
        super(EventType.NODE_SETTINGS_CHANGED);
    }

    @Override
    public boolean isPermitted(EventSubscriber subscriber) {
        return subscriber.isAdmin();
    }

}
