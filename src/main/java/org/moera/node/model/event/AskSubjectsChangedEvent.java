package org.moera.node.model.event;

import org.moera.node.auth.Scope;
import org.moera.node.auth.principal.Principal;

public class AskSubjectsChangedEvent extends Event {

    public AskSubjectsChangedEvent() {
        super(EventType.ASK_SUBJECTS_CHANGED, Scope.OTHER, Principal.ADMIN.not());
    }

}
