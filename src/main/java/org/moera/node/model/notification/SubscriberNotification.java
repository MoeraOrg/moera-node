package org.moera.node.model.notification;

public class SubscriberNotification extends Notification {

    private String subscriberId;

    public SubscriberNotification(NotificationType type) {
        super(type);
    }

    public String getSubscriberId() {
        return subscriberId;
    }

    public void setSubscriberId(String subscriberId) {
        this.subscriberId = subscriberId;
    }

}
