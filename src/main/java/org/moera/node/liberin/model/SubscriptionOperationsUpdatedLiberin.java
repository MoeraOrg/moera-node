package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.UserSubscription;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.SubscriptionInfoUtil;

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

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("subscription", SubscriptionInfoUtil.build(subscription, getPluginContext().getOptions()));
        model.put("latestViewPrincipal", latestViewPrincipal);
    }

}
