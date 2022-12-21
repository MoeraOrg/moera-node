package org.moera.node.liberin.receptor;

import javax.inject.Inject;

import org.moera.node.auth.principal.Principal;
import org.moera.node.auth.principal.PrincipalExpression;
import org.moera.node.auth.principal.PrincipalFilter;
import org.moera.node.data.SubscriptionType;
import org.moera.node.data.UserSubscription;
import org.moera.node.data.UserSubscriptionRepository;
import org.moera.node.liberin.Liberin;
import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.SubscriptionAddedLiberin;
import org.moera.node.liberin.model.SubscriptionDeletedLiberin;
import org.moera.node.liberin.model.SubscriptionOperationsUpdatedLiberin;
import org.moera.node.model.SubscriptionInfo;
import org.moera.node.model.event.SubscriptionAddedEvent;
import org.moera.node.model.event.SubscriptionDeletedEvent;
import org.moera.node.model.event.SubscriptionUpdatedEvent;
import org.moera.node.model.event.SubscriptionsTotalChangedEvent;
import org.moera.node.option.Options;

@LiberinReceptor
public class SubscriptionReceptor extends LiberinReceptorBase {

    @Inject
    private UserSubscriptionRepository userSubscriptionRepository;

    @LiberinMapping
    public void added(SubscriptionAddedLiberin liberin) {
        UserSubscription subscription = liberin.getSubscription();

        if (subscription.getSubscriptionType() == SubscriptionType.FEED) {
            send(liberin, new SubscriptionAddedEvent(new SubscriptionInfo(subscription, universalContext.getOptions()),
                    visibilityFilter(universalContext.getOptions(), subscription)));
            sendPeopleChangedEvent(liberin);
        }
    }

    @LiberinMapping
    public void operationsUpdated(SubscriptionOperationsUpdatedLiberin liberin) {
        UserSubscription subscription = liberin.getSubscription();

        if (subscription.getSubscriptionType() != SubscriptionType.FEED) {
            return;
        }

        PrincipalExpression addedFilter = generalVisibilityFilter(universalContext.getOptions(), subscription).a()
                .and(subscription.getViewE())
                .andNot(liberin.getLatestViewPrincipal());
        send(liberin, new SubscriptionAddedEvent(
                new SubscriptionInfo(subscription, universalContext.getOptions()), addedFilter));

        PrincipalExpression updatedFilter = generalVisibilityFilter(universalContext.getOptions(), subscription).a()
                .and(subscription.getViewE())
                .and(liberin.getLatestViewPrincipal());
        send(liberin, new SubscriptionUpdatedEvent(
                new SubscriptionInfo(subscription, universalContext.getOptions()), updatedFilter));

        PrincipalExpression deletedFilter = generalVisibilityFilter(universalContext.getOptions(), subscription).a()
                .andNot(subscription.getViewE())
                .and(liberin.getLatestViewPrincipal());
        send(liberin, new SubscriptionDeletedEvent(
                new SubscriptionInfo(subscription, universalContext.getOptions()), deletedFilter));
    }

    @LiberinMapping
    public void deleted(SubscriptionDeletedLiberin liberin) {
        UserSubscription subscription = liberin.getSubscription();

        if (subscription.getSubscriptionType() == SubscriptionType.FEED) {
            send(liberin, new SubscriptionDeletedEvent(
                    new SubscriptionInfo(subscription, universalContext.getOptions()),
                    visibilityFilter(universalContext.getOptions(), subscription)));
            sendPeopleChangedEvent(liberin);
        }
    }

    private void sendPeopleChangedEvent(Liberin liberin) {
        int subscriptionsTotal = userSubscriptionRepository.countByType(liberin.getNodeId(), SubscriptionType.FEED);
        send(liberin, new SubscriptionsTotalChangedEvent(subscriptionsTotal,
                totalVisibilityFilter(universalContext.getOptions())));
    }

    private PrincipalFilter generalVisibilityFilter(Options options, UserSubscription subscription) {
        return subscription.getSubscriptionType() == SubscriptionType.FEED
                ? UserSubscription.getViewAllE(options)
                : Principal.ofNode(subscription.getRemoteNodeName());
    }

    private PrincipalExpression visibilityFilter(Options options, UserSubscription subscription) {
        return generalVisibilityFilter(options, subscription).a().and(subscription.getViewE());
    }

    private PrincipalFilter totalVisibilityFilter(Options options) {
        return UserSubscription.getViewAllE(options).a()
                .or(UserSubscription.getViewTotalE(options));
    }

}
