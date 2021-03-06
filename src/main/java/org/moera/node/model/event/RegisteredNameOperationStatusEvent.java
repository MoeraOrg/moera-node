package org.moera.node.model.event;

import org.moera.node.event.EventSubscriber;

public class RegisteredNameOperationStatusEvent extends Event {

    public RegisteredNameOperationStatusEvent() {
        super(EventType.REGISTERED_NAME_OPERATION_STATUS);
    }

    @Override
    public boolean isPermitted(EventSubscriber subscriber) {
        return subscriber.isAdmin();
    }

}
