package org.moera.node.liberin.receptor;

import javax.inject.Inject;

import org.moera.node.auth.principal.PrincipalFilter;
import org.moera.node.data.Subscription;
import org.moera.node.data.SubscriptionRepository;
import org.moera.node.data.SubscriptionType;
import org.moera.node.liberin.Liberin;
import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.SubscriptionAddedLiberin;
import org.moera.node.liberin.model.SubscriptionDeletedLiberin;
import org.moera.node.model.event.SubscriptionAddedEvent;
import org.moera.node.model.event.SubscriptionDeletedEvent;
import org.moera.node.model.event.SubscriptionsTotalChangedEvent;
import org.moera.node.option.Options;

@LiberinReceptor
public class SubscriptionReceptor extends LiberinReceptorBase {

    @Inject
    private SubscriptionRepository subscriptionRepository;

    @LiberinMapping
    public void added(SubscriptionAddedLiberin liberin) {
        send(liberin, new SubscriptionAddedEvent(liberin.getSubscription(),
                visibilityFilter(universalContext.getOptions())));
        sendPeopleChangedEvent(liberin);
    }

    @LiberinMapping
    public void deleted(SubscriptionDeletedLiberin liberin) {
        send(liberin, new SubscriptionDeletedEvent(liberin.getSubscription(),
                visibilityFilter(universalContext.getOptions())));
        sendPeopleChangedEvent(liberin);
    }

    private void sendPeopleChangedEvent(Liberin liberin) {
        int subscriptionsTotal = subscriptionRepository.countByType(liberin.getNodeId(), SubscriptionType.FEED);
        send(liberin, new SubscriptionsTotalChangedEvent(subscriptionsTotal,
                totalVisibilityFilter(universalContext.getOptions())));
    }

    private PrincipalFilter visibilityFilter(Options options) {
        return Subscription.getViewAllE(options);
    }

    private PrincipalFilter totalVisibilityFilter(Options options) {
        return Subscription.getViewAllE(options).a()
                .or(Subscription.getViewTotalE(options));
    }

}
