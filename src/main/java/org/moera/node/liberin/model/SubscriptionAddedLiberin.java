package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.node.data.Subscription;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.SubscriptionInfo;

public class SubscriptionAddedLiberin extends Liberin {

    private Subscription subscription;

    public SubscriptionAddedLiberin(Subscription subscription) {
        this.subscription = subscription;
    }

    public Subscription getSubscription() {
        return subscription;
    }

    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("subscription", new SubscriptionInfo(subscription));
    }

}
