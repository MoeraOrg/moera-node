package org.moera.node.model.event;

import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.principal.Principal;

public class NodeSettingsChangedEvent extends Event {

    public NodeSettingsChangedEvent() {
        super(EventType.NODE_SETTINGS_CHANGED, Scope.VIEW_SETTINGS, Principal.ADMIN);
    }

}
