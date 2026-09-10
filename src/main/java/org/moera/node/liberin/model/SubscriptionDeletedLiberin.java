package org.moera.node.liberin.model;

import org.moera.node.data.UserSubscription;
import org.moera.node.liberin.Liberin;

public class SubscriptionDeletedLiberin extends Liberin {

    private UserSubscription subscription;

    public SubscriptionDeletedLiberin(UserSubscription subscription) {
        this.subscription = subscription;
    }

    public UserSubscription getSubscription() {
        return subscription;
    }

    public void setSubscription(UserSubscription subscription) {
        this.subscription = subscription;
    }

}
