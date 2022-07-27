package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.node.auth.principal.Principal;
import org.moera.node.data.Subscription;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.SubscriptionInfo;

public class SubscriptionOperationsUpdatedLiberin extends Liberin {

    private Subscription subscription;
    private Principal latestViewPrincipal;

    public SubscriptionOperationsUpdatedLiberin(Subscription subscription, Principal latestViewPrincipal) {
        this.subscription = subscription;
        this.latestViewPrincipal = latestViewPrincipal;
    }

    public Subscription getSubscription() {
        return subscription;
    }

    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
    }

    public Principal getLatestViewPrincipal() {
        return latestViewPrincipal;
    }

    public void setLatestViewPrincipal(Principal latestViewPrincipal) {
        this.latestViewPrincipal = latestViewPrincipal;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("subscription", new SubscriptionInfo(subscription));
        model.put("latestViewPrincipal", latestViewPrincipal);
    }

}
