package org.moera.node.model;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.UserSubscription;

public class SubscriptionOverride {

    private Map<String, Principal> operations;

    public Map<String, Principal> getOperations() {
        return operations;
    }

    public void setOperations(Map<String, Principal> operations) {
        this.operations = operations;
    }

    public Principal getPrincipal(String operationName) {
        return operations != null ? operations.get(operationName) : null;
    }

    public void toUserSubscription(UserSubscription subscription) {
        toPrincipal(this::getPrincipal, "view", subscription::setViewPrincipal);
    }

    private void toPrincipal(Function<String, Principal> getPrincipal, String operationName,
                             Consumer<Principal> setPrincipal) {
        Principal value = getPrincipal.apply(operationName);
        if (value != null) {
            setPrincipal.accept(value);
        }
    }

}
