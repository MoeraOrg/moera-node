package org.moera.node.rest.notification;

import javax.inject.Inject;

import org.moera.node.data.ContactRepository;
import org.moera.node.data.MediaFile;
import org.moera.node.data.SubscriberRepository;
import org.moera.node.data.Subscription;
import org.moera.node.data.SubscriptionRepository;
import org.moera.node.data.SubscriptionType;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.model.RemoteNodeAvatarChangedLiberin;
import org.moera.node.liberin.model.RemoteNodeFullNameChangedLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.model.AvatarImage;
import org.moera.node.model.UnsubscribeFailure;
import org.moera.node.model.notification.NotificationType;
import org.moera.node.model.notification.ProfileUpdatedNotification;
import org.moera.node.notification.receive.NotificationMapping;
import org.moera.node.notification.receive.NotificationProcessor;
import org.moera.node.util.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;

@NotificationProcessor
public class ProfileProcessor {

    private static final Logger log = LoggerFactory.getLogger(ProfileProcessor.class);

    @Inject
    private UniversalContext universalContext;

    @Inject
    private SubscriptionRepository subscriptionRepository;

    @Inject
    private SubscriberRepository subscriberRepository;

    @Inject
    private ContactRepository contactRepository;

    @Inject
    private MediaManager mediaManager;

    @Inject
    private PlatformTransactionManager txManager;

    private void validateSubscription(ProfileUpdatedNotification notification) {
        Subscription subscription = subscriptionRepository.findBySubscriber(
                universalContext.nodeId(), notification.getSenderNodeName(), notification.getSubscriberId()).orElse(null);
        if (subscription == null || subscription.getSubscriptionType() != SubscriptionType.PROFILE) {
            throw new UnsubscribeFailure();
        }
    }

    @NotificationMapping(NotificationType.PROFILE_UPDATED)
    public void profileUpdated(ProfileUpdatedNotification notification) {
        validateSubscription(notification);
        try {
            Transaction.execute(txManager, () -> {
                subscriberRepository.updateRemoteFullName(universalContext.nodeId(), notification.getSenderNodeName(),
                        notification.getSenderFullName());
                subscriptionRepository.updateRemoteFullName(universalContext.nodeId(), notification.getSenderNodeName(),
                        notification.getSenderFullName());
                contactRepository.updateRemoteFullName(universalContext.nodeId(), notification.getSenderNodeName(),
                        notification.getSenderFullName());
                return null;
            });
        } catch (Throwable e) {
            log.error("Error saving the full name: {}", e.getMessage());
        }
        universalContext.send(new RemoteNodeFullNameChangedLiberin(notification.getSenderNodeName(),
                notification.getSenderFullName()));

        mediaManager.asyncDownloadPublicMedia(notification.getSenderNodeName(),
                new AvatarImage[] {notification.getSenderAvatar()},
                mediaFiles -> this.saveAvatar(notification.getSenderNodeName(), mediaFiles[0],
                        notification.getSenderAvatar() != null ? notification.getSenderAvatar().getShape() : null));
    }

    private void saveAvatar(String nodeName, MediaFile mediaFile, String shape) {
        if (mediaFile == null) {
            return;
        }
        try {
            Transaction.execute(txManager, () -> {
                subscriberRepository.updateRemoteAvatar(universalContext.nodeId(), nodeName, mediaFile, shape);
                subscriptionRepository.updateRemoteAvatar(universalContext.nodeId(), nodeName, mediaFile, shape);
                contactRepository.updateRemoteAvatar(universalContext.nodeId(), nodeName, mediaFile, shape);
                return null;
            });
            universalContext.send(new RemoteNodeAvatarChangedLiberin(nodeName, new AvatarImage(mediaFile, shape)));
        } catch (Throwable e) {
            log.error("Error saving the downloaded avatar: {}", e.getMessage());
        }
    }

}
