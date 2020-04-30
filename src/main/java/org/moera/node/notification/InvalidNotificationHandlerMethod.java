package org.moera.node.notification;

public class InvalidNotificationHandlerMethod extends RuntimeException {

    public InvalidNotificationHandlerMethod(NotificationType type) {
        super(String.format("Notification handler method for type %s must have maximum 1 parameter of type %s",
                type.name(), type.getStructure()));
    }

}
