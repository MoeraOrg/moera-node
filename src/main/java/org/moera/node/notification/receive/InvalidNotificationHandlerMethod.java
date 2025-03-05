package org.moera.node.notification.receive;

import org.moera.lib.node.types.notifications.NotificationType;

public class InvalidNotificationHandlerMethod extends RuntimeException {

    public InvalidNotificationHandlerMethod(NotificationType type) {
        super(
            "Notification handler method for type %s must have maximum 1 parameter of type %s"
                .formatted(type.name(), type.getStructure())
        );
    }

}
