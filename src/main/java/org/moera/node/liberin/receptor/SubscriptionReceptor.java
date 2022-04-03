package org.moera.node.liberin.receptor;

import javax.inject.Inject;

import org.moera.node.data.SubscriberRepository;
import org.moera.node.data.SubscriptionRepository;
import org.moera.node.data.SubscriptionType;
import org.moera.node.liberin.Liberin;
import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.SubscriptionAddedLiberin;
import org.moera.node.liberin.model.SubscriptionDeletedLiberin;
import org.moera.node.model.event.PeopleChangedEvent;
import org.moera.node.model.event.SubscriptionAddedEvent;
import org.moera.node.model.event.SubscriptionDeletedEvent;

@LiberinReceptor
public class SubscriptionReceptor extends LiberinReceptorBase {

    @Inject
    private SubscriberRepository subscriberRepository;

    @Inject
    private SubscriptionRepository subscriptionRepository;

    @LiberinMapping
    public void added(SubscriptionAddedLiberin liberin) {
        send(liberin, new SubscriptionAddedEvent(liberin.getSubscription()));
        sendPeopleChangedEvent(liberin);
    }

    @LiberinMapping
    public void deleted(SubscriptionDeletedLiberin liberin) {
        send(liberin, new SubscriptionDeletedEvent(liberin.getSubscription()));
        sendPeopleChangedEvent(liberin);
    }

    private void sendPeopleChangedEvent(Liberin liberin) {
        int subscribersTotal = subscriberRepository.countAllByType(liberin.getNodeId(), SubscriptionType.FEED);
        int subscriptionsTotal = subscriptionRepository.countByType(liberin.getNodeId(), SubscriptionType.FEED);
        send(liberin, new PeopleChangedEvent(subscribersTotal, subscriptionsTotal));
    }

}
