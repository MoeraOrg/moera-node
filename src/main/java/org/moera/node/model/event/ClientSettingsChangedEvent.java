package org.moera.node.model.event;

import org.moera.node.auth.Scope;
import org.moera.node.auth.principal.Principal;

public class ClientSettingsChangedEvent extends Event {

    public ClientSettingsChangedEvent() {
        super(EventType.CLIENT_SETTINGS_CHANGED, Scope.VIEW_SETTINGS, Principal.ADMIN);
    }

}
