package org.moera.node.model.notification;

public abstract class SubscriberNotification extends Notification {

    private String subscriberId;

    protected SubscriberNotification(NotificationType type) {
        super(type);
    }

    public String getSubscriberId() {
        return subscriberId;
    }

    public void setSubscriberId(String subscriberId) {
        this.subscriberId = subscriberId;
    }

}
