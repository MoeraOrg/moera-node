package org.moera.node.rest.notification;

import jakarta.inject.Inject;

import org.moera.lib.node.types.SubscriptionType;
import org.moera.lib.node.types.notifications.NotificationType;
import org.moera.lib.node.types.notifications.ProfileUpdatedNotification;
import org.moera.node.data.Subscription;
import org.moera.node.data.SubscriptionRepository;
import org.moera.node.global.UniversalContext;
import org.moera.node.model.UnsubscribeFailure;
import org.moera.node.notification.receive.NotificationMapping;
import org.moera.node.notification.receive.NotificationProcessor;
import org.moera.node.task.Jobs;
import org.moera.node.util.Transaction;

@NotificationProcessor
public class ProfileProcessor {

    @Inject
    private UniversalContext universalContext;

    @Inject
    private SubscriptionRepository subscriptionRepository;

    @Inject
    private Transaction tx;

    @Inject
    private Jobs jobs;

    private void validateSubscription(ProfileUpdatedNotification notification) {
        Subscription subscription = subscriptionRepository.findBySubscriber(
            universalContext.nodeId(), notification.getSenderNodeName(), notification.getSubscriberId()
        ).orElse(null);
        if (subscription == null || subscription.getSubscriptionType() != SubscriptionType.PROFILE) {
            throw new UnsubscribeFailure();
        }
    }

    @NotificationMapping(NotificationType.PROFILE_UPDATED)
    public void profileUpdated(ProfileUpdatedNotification notification) {
        tx.executeRead(() -> validateSubscription(notification));
        jobs.run(
            ProfileUpdateJob.class,
            new ProfileUpdateJob.Parameters(notification.getSenderNodeName()),
            universalContext.nodeId()
        );
    }

}
