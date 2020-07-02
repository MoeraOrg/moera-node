package org.moera.node.model.notification;

import java.sql.Timestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class SubscriberNotification extends Notification {

    private String subscriberId;

    @JsonIgnore
    private Timestamp subscriptionCreatedAt;

    protected SubscriberNotification(NotificationType type) {
        super(type);
    }

    public String getSubscriberId() {
        return subscriberId;
    }

    public void setSubscriberId(String subscriberId) {
        this.subscriberId = subscriberId;
    }

    public Timestamp getSubscriptionCreatedAt() {
        return subscriptionCreatedAt;
    }

    public void setSubscriptionCreatedAt(Timestamp subscriptionCreatedAt) {
        this.subscriptionCreatedAt = subscriptionCreatedAt;
    }

}
