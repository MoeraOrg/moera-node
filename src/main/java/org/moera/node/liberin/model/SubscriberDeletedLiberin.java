package org.moera.node.liberin.model;

import org.moera.node.data.Subscriber;
import org.moera.node.liberin.Liberin;

public class SubscriberDeletedLiberin extends Liberin {

    private Subscriber subscriber;

    public SubscriberDeletedLiberin(Subscriber subscriber) {
        this.subscriber = subscriber;
    }

    public Subscriber getSubscriber() {
        return subscriber;
    }

    public void setSubscriber(Subscriber subscriber) {
        this.subscriber = subscriber;
    }

}
