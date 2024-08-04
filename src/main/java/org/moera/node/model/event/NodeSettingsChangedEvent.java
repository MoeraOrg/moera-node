package org.moera.node.model.event;

import org.moera.node.auth.Scope;
import org.moera.node.auth.principal.Principal;

public class NodeSettingsChangedEvent extends Event {

    public NodeSettingsChangedEvent() {
        super(EventType.NODE_SETTINGS_CHANGED, Scope.VIEW_SETTINGS, Principal.ADMIN);
    }

}
