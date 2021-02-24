package org.moera.node.rest.notification;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.moera.node.data.ContactRepository;
import org.moera.node.data.SubscriberRepository;
import org.moera.node.data.Subscription;
import org.moera.node.data.SubscriptionRepository;
import org.moera.node.data.SubscriptionType;
import org.moera.node.global.RequestContext;
import org.moera.node.model.UnsubscribeFailure;
import org.moera.node.model.event.RemoteNodeFullNameChangedEvent;
import org.moera.node.model.notification.NotificationType;
import org.moera.node.model.notification.ProfileUpdatedNotification;
import org.moera.node.notification.receive.NotificationMapping;
import org.moera.node.notification.receive.NotificationProcessor;

@NotificationProcessor
public class ProfileProcessor {

    @Inject
    private RequestContext requestContext;

    @Inject
    private SubscriptionRepository subscriptionRepository;

    @Inject
    private SubscriberRepository subscriberRepository;

    @Inject
    private ContactRepository contactRepository;

    private void validateSubscription(ProfileUpdatedNotification notification) {
        Subscription subscription = subscriptionRepository.findBySubscriber(
                requestContext.nodeId(), notification.getSenderNodeName(), notification.getSubscriberId()).orElse(null);
        if (subscription == null || subscription.getSubscriptionType() != SubscriptionType.PROFILE) {
            throw new UnsubscribeFailure();
        }
    }

    @NotificationMapping(NotificationType.PROFILE_UPDATED)
    @Transactional
    public void profileUpdated(ProfileUpdatedNotification notification) {
        validateSubscription(notification);
        subscriberRepository.updateRemoteFullName(requestContext.nodeId(), notification.getSenderNodeName(),
                notification.getSenderFullName());
        subscriptionRepository.updateRemoteFullName(requestContext.nodeId(), notification.getSenderNodeName(),
                notification.getSenderFullName());
        contactRepository.updateRemoteFullName(requestContext.nodeId(), notification.getSenderNodeName(),
                notification.getSenderFullName());
        requestContext.send(new RemoteNodeFullNameChangedEvent(notification.getSenderNodeName(),
                notification.getSenderFullName()));
    }

}
