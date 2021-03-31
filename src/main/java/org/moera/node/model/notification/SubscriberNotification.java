package org.moera.node.model.notification;

import java.sql.Timestamp;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.moera.commons.util.LogUtil;
import org.springframework.data.util.Pair;

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

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("subscriberId", LogUtil.format(subscriberId)));
    }

}
