package org.moera.node.liberin.model;

import org.moera.node.data.Subscriber;
import org.moera.node.liberin.Liberin;

public class SubscriberAddedLiberin extends Liberin {

    private Subscriber subscriber;
    private Long subscriberLastUpdatedAt;

    public SubscriberAddedLiberin(Subscriber subscriber, Long subscriberLastUpdatedAt) {
        this.subscriber = subscriber;
        this.subscriberLastUpdatedAt = subscriberLastUpdatedAt;
    }

    public Subscriber getSubscriber() {
        return subscriber;
    }

    public void setSubscriber(Subscriber subscriber) {
        this.subscriber = subscriber;
    }

    public Long getSubscriberLastUpdatedAt() {
        return subscriberLastUpdatedAt;
    }

    public void setSubscriberLastUpdatedAt(Long subscriberLastUpdatedAt) {
        this.subscriberLastUpdatedAt = subscriberLastUpdatedAt;
    }

}
