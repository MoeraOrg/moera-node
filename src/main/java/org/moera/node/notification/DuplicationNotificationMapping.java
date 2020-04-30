package org.moera.node.notification;

import java.lang.reflect.Method;

public class DuplicationNotificationMapping extends RuntimeException {

    public DuplicationNotificationMapping(NotificationType type, Method method) {
        super(String.format("Notification mapping for type %s is already declared on method %s", type.name(), method));
    }

}
