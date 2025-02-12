package org.moera.node.model.event;

import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.principal.Principal;

public class PluginsUpdatedEvent extends Event {

    public PluginsUpdatedEvent() {
        super(EventType.PLUGINS_UPDATED, Scope.IDENTIFY, Principal.ADMIN);
    }

}
