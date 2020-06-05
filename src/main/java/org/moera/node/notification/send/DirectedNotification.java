package org.moera.node.notification.send;

import org.moera.node.model.notification.Notification;

public class DirectedNotification {

    private Direction direction;
    private Notification notification;

    public DirectedNotification(Direction direction, Notification notification) {
        this.direction = direction;
        this.notification = notification;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public Notification getNotification() {
        return notification;
    }

    public void setNotification(Notification notification) {
        this.notification = notification;
    }

}
