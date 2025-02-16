package org.moera.node.model;

import java.util.function.Consumer;

import org.moera.lib.node.types.SubscriberOverride;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.Subscriber;

public class SubscriberOverrideUtil {

    public static void toSubscriber(SubscriberOverride override, Subscriber subscriber) {
        if (override.getOperations() != null) {
            toPrincipal(override.getOperations().getView(), subscriber::setViewPrincipal);
        }

        if (override.getAdminOperations() != null) {
            toPrincipal(override.getAdminOperations().getView(), subscriber::setAdminViewPrincipal);
        }
    }

    private static void toPrincipal(Principal value, Consumer<Principal> setPrincipal) {
        if (value != null) {
            setPrincipal.accept(value);
        }
    }

}
