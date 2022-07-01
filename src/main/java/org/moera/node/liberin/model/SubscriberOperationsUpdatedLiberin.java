package org.moera.node.liberin.model;

import org.moera.node.auth.principal.Principal;
import org.moera.node.data.Subscriber;
import org.moera.node.liberin.Liberin;

public class SubscriberOperationsUpdatedLiberin extends Liberin {

    private Subscriber subscriber;
    private Principal latestViewPrincipal;

    public SubscriberOperationsUpdatedLiberin(Subscriber subscriber, Principal latestViewPrincipal) {
        this.subscriber = subscriber;
        this.latestViewPrincipal = latestViewPrincipal;
    }

    public Subscriber getSubscriber() {
        return subscriber;
    }

    public void setSubscriber(Subscriber subscriber) {
        this.subscriber = subscriber;
    }

    public Principal getLatestViewPrincipal() {
        return latestViewPrincipal;
    }

    public void setLatestViewPrincipal(Principal latestViewPrincipal) {
        this.latestViewPrincipal = latestViewPrincipal;
    }

}
