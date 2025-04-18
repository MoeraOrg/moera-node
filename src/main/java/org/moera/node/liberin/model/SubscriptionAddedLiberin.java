package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.node.data.UserSubscription;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.SubscriptionInfoUtil;

public class SubscriptionAddedLiberin extends Liberin {

    private UserSubscription subscription;

    public SubscriptionAddedLiberin(UserSubscription subscription) {
        this.subscription = subscription;
    }

    public UserSubscription getSubscription() {
        return subscription;
    }

    public void setSubscription(UserSubscription subscription) {
        this.subscription = subscription;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("subscription", SubscriptionInfoUtil.build(subscription, getPluginContext().getOptions()));
    }

}
