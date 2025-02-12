package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.lib.node.types.principal.AccessCheckers;
import org.moera.node.data.Subscriber;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.SubscriberInfoUtil;

public class SubscriberAddedLiberin extends Liberin {

    private Subscriber subscriber;
    private Long subscriberLastUpdatedAt;

    public SubscriberAddedLiberin(Subscriber subscriber, Long subscriberLastUpdatedAt) {
        this.subscriber = subscriber;
        this.subscriberLastUpdatedAt = subscriberLastUpdatedAt;
    }

    public Subscriber getSubscriber() {
        return subscriber;
    }

    public void setSubscriber(Subscriber subscriber) {
        this.subscriber = subscriber;
    }

    public Long getSubscriberLastUpdatedAt() {
        return subscriberLastUpdatedAt;
    }

    public void setSubscriberLastUpdatedAt(Long subscriberLastUpdatedAt) {
        this.subscriberLastUpdatedAt = subscriberLastUpdatedAt;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put(
            "subscriber",
            SubscriberInfoUtil.build(subscriber, getPluginContext().getOptions(), AccessCheckers.ADMIN)
        );
        model.put("subscriberLastUpdatedAt", subscriberLastUpdatedAt);
    }

}
