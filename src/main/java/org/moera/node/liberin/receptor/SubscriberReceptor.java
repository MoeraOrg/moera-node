package org.moera.node.liberin.receptor;

import jakarta.inject.Inject;

import org.moera.lib.node.types.SubscriptionType;
import org.moera.lib.node.types.principal.AccessCheckers;
import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.node.types.principal.PrincipalExpression;
import org.moera.lib.node.types.principal.PrincipalFilter;
import org.moera.node.data.Subscriber;
import org.moera.node.data.SubscriberRepository;
import org.moera.node.instant.SubscriberInstants;
import org.moera.node.liberin.Liberin;
import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.SubscriberAddedLiberin;
import org.moera.node.liberin.model.SubscriberDeletedLiberin;
import org.moera.node.liberin.model.SubscriberOperationsUpdatedLiberin;
import org.moera.node.model.SubscriberInfoUtil;
import org.moera.node.model.event.SubscriberAddedEvent;
import org.moera.node.model.event.SubscriberDeletedEvent;
import org.moera.node.model.event.SubscriberUpdatedEvent;
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

        if (subscriber.getSubscriptionType() == SubscriptionType.FEED) {
            send(liberin, new SubscriberAddedEvent(
                SubscriberInfoUtil.build(subscriber, universalContext.getOptions(), AccessCheckers.PUBLIC),
                visibilityFilter(universalContext.getOptions(), subscriber)
            ));
            send(liberin, new SubscriberAddedEvent(
                SubscriberInfoUtil.build(
                    subscriber,
                    universalContext.getOptions(),
                    AccessCheckers.node(subscriber.getRemoteNodeName())
                ),
                Principal.ofNode(subscriber.getRemoteNodeName())
            ));
            sendPeopleChangedEvent(liberin);
        }

        subscriberInstants.added(subscriber);
    }

    @LiberinMapping
    public void operationsUpdated(SubscriberOperationsUpdatedLiberin liberin) {
        Subscriber subscriber = liberin.getSubscriber();

        if (subscriber.getSubscriptionType() != SubscriptionType.FEED) {
            return;
        }

        PrincipalExpression addedFilter = generalVisibilityFilter(universalContext.getOptions(), subscriber)
                .and(subscriber.getViewE())
                .andNot(liberin.getLatestViewPrincipal());
        send(liberin, new SubscriberAddedEvent(
            SubscriberInfoUtil.build(subscriber, universalContext.getOptions(), AccessCheckers.PUBLIC), addedFilter
        ));

        PrincipalExpression updatedFilter = generalVisibilityFilter(universalContext.getOptions(), subscriber)
                .and(subscriber.getViewE())
                .and(liberin.getLatestViewPrincipal());
        send(liberin, new SubscriberUpdatedEvent(
            SubscriberInfoUtil.build(subscriber, universalContext.getOptions(), AccessCheckers.PUBLIC), updatedFilter
        ));
        send(liberin, new SubscriberUpdatedEvent(
            SubscriberInfoUtil.build(
                subscriber,
                universalContext.getOptions(),
                AccessCheckers.node(subscriber.getRemoteNodeName())
            ),
            Principal.ofNode(subscriber.getRemoteNodeName())
        ));

        PrincipalExpression deletedFilter = generalVisibilityFilter(universalContext.getOptions(), subscriber)
                .andNot(subscriber.getViewE())
                .and(liberin.getLatestViewPrincipal());
        send(liberin, new SubscriberDeletedEvent(
            SubscriberInfoUtil.build(subscriber, universalContext.getOptions(), AccessCheckers.PUBLIC), deletedFilter
        ));
    }

    @LiberinMapping
    public void deleted(SubscriberDeletedLiberin liberin) {
        Subscriber subscriber = liberin.getSubscriber();

        if (subscriber.getSubscriptionType() == SubscriptionType.FEED) {
            send(liberin, new SubscriberDeletedEvent(
                SubscriberInfoUtil.build(subscriber, universalContext.getOptions(), AccessCheckers.PUBLIC),
                visibilityFilter(universalContext.getOptions(), subscriber)
            ));
            send(liberin, new SubscriberDeletedEvent(
                SubscriberInfoUtil.build(
                    subscriber,
                    universalContext.getOptions(),
                    AccessCheckers.node(subscriber.getRemoteNodeName())
                ),
                Principal.ofNode(subscriber.getRemoteNodeName())
            ));
            sendPeopleChangedEvent(liberin);
        }

        subscriberInstants.deleted(liberin.getSubscriber());
    }

    private void sendPeopleChangedEvent(Liberin liberin) {
        int subscribersTotal = subscriberRepository.countAllByType(liberin.getNodeId(), SubscriptionType.FEED);
        send(liberin, new SubscribersTotalChangedEvent(subscribersTotal,
                totalVisibilityFilter(universalContext.getOptions())));
    }

    private PrincipalExpression generalVisibilityFilter(Options options, Subscriber subscriber) {
        return subscriber.getSubscriptionType() == SubscriptionType.FEED
                ? Subscriber.getViewAllE(options).a().andNot(Principal.ofNode(subscriber.getRemoteNodeName()))
                : Principal.ADMIN.a();
    }

    private PrincipalExpression visibilityFilter(Options options, Subscriber subscriber) {
        return generalVisibilityFilter(options, subscriber).and(subscriber.getViewE());
    }

    private PrincipalFilter totalVisibilityFilter(Options options) {
        return Subscriber.getViewAllE(options).a()
                .or(Subscriber.getViewTotalE(options));
    }

}
