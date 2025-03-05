package org.moera.node.notification.receive;

import java.lang.reflect.Method;

import org.moera.lib.node.types.notifications.NotificationType;

public class DuplicationNotificationMapping extends RuntimeException {

    public DuplicationNotificationMapping(NotificationType type, Method method) {
        super("Notification mapping for type %s is already declared on method %s".formatted(type.name(), method));
    }

}
