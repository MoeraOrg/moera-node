package org.moera.node.model.event;

import org.moera.lib.node.types.Scope;
import org.moera.node.auth.principal.Principal;

public class RegisteredNameOperationStatusEvent extends Event {

    public RegisteredNameOperationStatusEvent() {
        super(EventType.REGISTERED_NAME_OPERATION_STATUS, Scope.NAME, Principal.ADMIN);
    }

}
