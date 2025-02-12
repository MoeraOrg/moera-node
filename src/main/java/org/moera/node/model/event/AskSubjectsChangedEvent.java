package org.moera.node.model.event;

import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.principal.Principal;

public class AskSubjectsChangedEvent extends Event {

    public AskSubjectsChangedEvent() {
        super(EventType.ASK_SUBJECTS_CHANGED, Scope.OTHER, Principal.ADMIN.not());
    }

}
