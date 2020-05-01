package org.moera.node.notification.receive;

import java.lang.reflect.Method;

import org.moera.node.notification.NotificationType;

public class DuplicationNotificationMapping extends RuntimeException {

    public DuplicationNotificationMapping(NotificationType type, Method method) {
        super(String.format("Notification mapping for type %s is already declared on method %s", type.name(), method));
    }

}
