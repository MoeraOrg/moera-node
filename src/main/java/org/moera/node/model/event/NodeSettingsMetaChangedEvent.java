package org.moera.node.model.event;

import org.moera.node.event.EventSubscriber;

public class NodeSettingsMetaChangedEvent extends Event {

    public NodeSettingsMetaChangedEvent() {
        super(EventType.NODE_SETTINGS_META_CHANGED);
    }

    @Override
    public boolean isPermitted(EventSubscriber subscriber) {
        return subscriber.isAdmin();
    }

}
