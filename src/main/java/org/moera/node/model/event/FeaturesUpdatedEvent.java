package org.moera.node.model.event;

import org.moera.node.auth.principal.Principal;

public class FeaturesUpdatedEvent extends Event {

    public FeaturesUpdatedEvent() {
        this(null);
    }

    public FeaturesUpdatedEvent(String clientName) {
        super(EventType.FEATURES_UPDATED, clientName != null ? Principal.ofNode(clientName) : Principal.PUBLIC);
    }

}
