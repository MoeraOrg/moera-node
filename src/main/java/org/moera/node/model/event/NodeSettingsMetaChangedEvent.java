package org.moera.node.model.event;

import org.moera.lib.node.types.Scope;
import org.moera.node.auth.principal.Principal;

public class NodeSettingsMetaChangedEvent extends Event {

    public NodeSettingsMetaChangedEvent() {
        super(EventType.NODE_SETTINGS_META_CHANGED, Scope.VIEW_SETTINGS, Principal.ADMIN);
    }

}
