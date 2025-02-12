package org.moera.node.model;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.Subscriber;

public class SubscriberOverride {

    private Map<String, Principal> operations;

    private Map<String, Principal> adminOperations;

    public Map<String, Principal> getOperations() {
        return operations;
    }

    public void setOperations(Map<String, Principal> operations) {
        this.operations = operations;
    }

    public Principal getPrincipal(String operationName) {
        return operations != null ? operations.get(operationName) : null;
    }

    public Map<String, Principal> getAdminOperations() {
        return adminOperations;
    }

    public void setAdminOperations(Map<String, Principal> adminOperations) {
        this.adminOperations = adminOperations;
    }

    public Principal getAdminPrincipal(String operationName) {
        return adminOperations != null ? adminOperations.get(operationName) : null;
    }

    public void toSubscriber(Subscriber subscriber) {
        toPrincipal(this::getPrincipal, "view", subscriber::setViewPrincipal);

        toPrincipal(this::getAdminPrincipal, "view", subscriber::setAdminViewPrincipal);
    }

    private void toPrincipal(Function<String, Principal> getPrincipal, String operationName,
                             Consumer<Principal> setPrincipal) {
        Principal value = getPrincipal.apply(operationName);
        if (value != null) {
            setPrincipal.accept(value);
        }
    }

}
