package org.moera.node.model.event;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.principal.PrincipalFilter;
import org.moera.lib.util.LogUtil;
import org.moera.node.model.SubscriberInfo;
import org.springframework.data.util.Pair;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubscriberEvent extends Event {

    private SubscriberInfo subscriber;

    public SubscriberEvent(EventType type) {
        super(type, Scope.VIEW_PEOPLE);
    }

    public SubscriberEvent(EventType type, SubscriberInfo subscriber, PrincipalFilter filter) {
        super(type, Scope.VIEW_PEOPLE, filter);
        this.subscriber = subscriber;
    }

    public SubscriberInfo getSubscriber() {
        return subscriber;
    }

    public void setSubscriber(SubscriberInfo subscriber) {
        this.subscriber = subscriber;
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("subscriptionType", LogUtil.format(subscriber.getType().toString())));
        parameters.add(Pair.of("feedName", LogUtil.format(subscriber.getFeedName())));
        parameters.add(Pair.of("postingId", LogUtil.format(subscriber.getPostingId())));
        parameters.add(Pair.of("nodeName", LogUtil.format(subscriber.getNodeName())));
    }

}
