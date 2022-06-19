package org.moera.node.liberin.receptor;

import javax.inject.Inject;

import org.moera.node.auth.principal.PrincipalFilter;
import org.moera.node.data.Subscriber;
import org.moera.node.data.SubscriberRepository;
import org.moera.node.data.SubscriptionType;
import org.moera.node.instant.SubscriberInstants;
import org.moera.node.liberin.Liberin;
import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.SubscriberAddedLiberin;
import org.moera.node.liberin.model.SubscriberDeletedLiberin;
import org.moera.node.model.event.SubscriberAddedEvent;
import org.moera.node.model.event.SubscriberDeletedEvent;
import org.moera.node.model.event.SubscribersTotalChangedEvent;
import org.moera.node.model.notification.PostingUpdatedNotification;
import org.moera.node.notification.send.Directions;
import org.moera.node.option.Options;
import org.moera.node.util.Util;

@LiberinReceptor
public class SubscriberReceptor extends LiberinReceptorBase {

    @Inject
    private SubscriberRepository subscriberRepository;

    @Inject
    private SubscriberInstants subscriberInstants;

    @LiberinMapping
    public void added(SubscriberAddedLiberin liberin) {
        Subscriber subscriber = liberin.getSubscriber();

        if (subscriber.getSubscriptionType() == SubscriptionType.POSTING && subscriber.getEntry() != null
                && !Util.toEpochSecond(subscriber.getEntry().getEditedAt())
                        .equals(liberin.getSubscriberLastUpdatedAt())) {
            PostingUpdatedNotification notification = new PostingUpdatedNotification(subscriber.getEntry().getId());
            notification.setSubscriberId(subscriber.getId().toString());
            notification.setSubscriptionCreatedAt(Util.now());
            send(Directions.single(liberin.getNodeId(), subscriber.getRemoteNodeName()), notification);
        }
        send(liberin, new SubscriberAddedEvent(subscriber, visibilityFilter(universalContext.getOptions())));
        sendPeopleChangedEvent(liberin);
        subscriberInstants.added(subscriber);
    }

    @LiberinMapping
    public void deleted(SubscriberDeletedLiberin liberin) {
        send(liberin, new SubscriberDeletedEvent(liberin.getSubscriber(),
                visibilityFilter(universalContext.getOptions())));
        sendPeopleChangedEvent(liberin);
        subscriberInstants.deleted(liberin.getSubscriber());
    }

    private void sendPeopleChangedEvent(Liberin liberin) {
        int subscribersTotal = subscriberRepository.countAllByType(liberin.getNodeId(), SubscriptionType.FEED);
        send(liberin, new SubscribersTotalChangedEvent(subscribersTotal,
                totalVisibilityFilter(universalContext.getOptions())));
    }

    private PrincipalFilter visibilityFilter(Options options) {
        return options.getPrincipal("subscribers.view");
    }

    private PrincipalFilter totalVisibilityFilter(Options options) {
        return options.getPrincipal("subscribers.view").a()
                .or(options.getPrincipal("subscribers.view-total"));
    }

}
