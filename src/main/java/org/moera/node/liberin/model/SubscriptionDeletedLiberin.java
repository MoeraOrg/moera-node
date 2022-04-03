package org.moera.node.liberin.model;

import org.moera.node.data.Subscription;
import org.moera.node.liberin.Liberin;

public class SubscriptionDeletedLiberin extends Liberin {

    private Subscription subscription;

    public SubscriptionDeletedLiberin(Subscription subscription) {
        this.subscription = subscription;
    }

    public Subscription getSubscription() {
        return subscription;
    }

    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
    }

}
