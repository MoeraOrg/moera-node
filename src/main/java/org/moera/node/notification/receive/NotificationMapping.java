package org.moera.node.notification.receive;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.moera.lib.node.types.notifications.NotificationType;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NotificationMapping {

    NotificationType value();

}
