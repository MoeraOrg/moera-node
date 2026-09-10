package org.moera.node.liberin.model;

import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.UserSubscription;
import org.moera.node.liberin.Liberin;

public class SubscriptionOperationsUpdatedLiberin extends Liberin {

    private UserSubscription subscription;
    private Principal latestViewPrincipal;

    public SubscriptionOperationsUpdatedLiberin(UserSubscription subscription, Principal latestViewPrincipal) {
        this.subscription = subscription;
        this.latestViewPrincipal = latestViewPrincipal;
    }

    public UserSubscription getSubscription() {
        return subscription;
    }

    public void setSubscription(UserSubscription subscription) {
        this.subscription = subscription;
    }

    public Principal getLatestViewPrincipal() {
        return latestViewPrincipal;
    }

    public void setLatestViewPrincipal(Principal latestViewPrincipal) {
        this.latestViewPrincipal = latestViewPrincipal;
    }

}
