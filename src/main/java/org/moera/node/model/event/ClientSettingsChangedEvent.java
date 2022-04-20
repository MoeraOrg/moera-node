package org.moera.node.model.event;

import org.moera.node.auth.principal.Principal;

public class ClientSettingsChangedEvent extends Event {

    public ClientSettingsChangedEvent() {
        super(EventType.CLIENT_SETTINGS_CHANGED, Principal.ADMIN);
    }

}
