package org.moera.node.notification.send;

import org.moera.node.model.notification.Notification;

@FunctionalInterface
@Deprecated
public interface NotificationConsumer {

    @Deprecated
    void send(DirectedNotification directedNotification);

    @Deprecated
    default void send(Direction direction, Notification notification) {
        send(new DirectedNotification(direction, notification));
    }

}
