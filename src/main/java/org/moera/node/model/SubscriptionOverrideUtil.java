package org.moera.node.model;

import java.util.function.Consumer;

import org.moera.lib.node.types.SubscriptionOverride;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.UserSubscription;

public class SubscriptionOverrideUtil {

    public static void toUserSubscription(SubscriptionOverride override, UserSubscription subscription) {
        if (override.getOperations() != null) {
            toPrincipal(override.getOperations().getView(), subscription::setViewPrincipal);
        }
    }

    private static void toPrincipal(Principal value, Consumer<Principal> setPrincipal) {
        if (value != null) {
            setPrincipal.accept(value);
        }
    }

}
