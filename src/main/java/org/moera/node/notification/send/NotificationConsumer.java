package org.moera.node.notification.send;

import org.moera.node.model.notification.Notification;

@FunctionalInterface
public interface NotificationConsumer {

    void send(DirectedNotification directedNotification);

    default void send(Direction direction, Notification notification) {
        send(new DirectedNotification(direction, notification));
    }

}
