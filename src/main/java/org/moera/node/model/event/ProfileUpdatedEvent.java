package org.moera.node.model.event;

import org.moera.node.auth.Scope;

public class ProfileUpdatedEvent extends Event {

    public ProfileUpdatedEvent() {
        super(EventType.PROFILE_UPDATED, Scope.VIEW_PROFILE);
    }

}
