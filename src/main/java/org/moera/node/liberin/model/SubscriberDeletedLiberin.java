package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.node.auth.principal.AccessCheckers;
import org.moera.node.data.Subscriber;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.SubscriberInfo;

public class SubscriberDeletedLiberin extends Liberin {

    private Subscriber subscriber;

    public SubscriberDeletedLiberin(Subscriber subscriber) {
        this.subscriber = subscriber;
    }

    public Subscriber getSubscriber() {
        return subscriber;
    }

    public void setSubscriber(Subscriber subscriber) {
        this.subscriber = subscriber;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("subscriber", new SubscriberInfo(subscriber, getPluginContext().getOptions(), AccessCheckers.ADMIN));
    }

}
