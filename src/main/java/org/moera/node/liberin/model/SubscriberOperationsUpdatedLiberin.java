package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.lib.node.types.principal.AccessCheckers;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.Subscriber;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.SubscriberInfo;

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

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("subscriber", new SubscriberInfo(subscriber, getPluginContext().getOptions(), AccessCheckers.ADMIN));
        model.put("latestViewPrincipal", latestViewPrincipal);
    }

}
