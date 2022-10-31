package org.moera.node.rest.notification;

import javax.inject.Inject;

import org.moera.node.data.ContactRepository;
import org.moera.node.data.MediaFile;
import org.moera.node.data.SubscriberRepository;
import org.moera.node.data.Subscription;
import org.moera.node.data.SubscriptionRepository;
import org.moera.node.data.SubscriptionType;
import org.moera.node.data.UserSubscriptionRepository;
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
    private UserSubscriptionRepository userSubscriptionRepository;

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
        updateProfileDetails(notification.getSenderNodeName(), notification.getSenderFullName(),
                notification.getSenderGender(), notification.getSenderAvatar());
    }

    public void updateProfileDetails(String nodeName, String fullName, String gender, AvatarImage avatar) {
        try {
            Transaction.execute(txManager, () -> {
                subscriberRepository.updateRemoteFullNameAndGender(
                        universalContext.nodeId(), nodeName, fullName, gender);
                userSubscriptionRepository.updateRemoteFullNameAndGender(
                        universalContext.nodeId(), nodeName, fullName, gender);
                contactRepository.updateRemoteFullNameAndGender(
                        universalContext.nodeId(), nodeName, fullName, gender);
                return null;
            });
        } catch (Throwable e) {
            log.error("Error saving the full name: {}", e.getMessage());
        }
        universalContext.send(new RemoteNodeFullNameChangedLiberin(nodeName, fullName));

        mediaManager.asyncDownloadPublicMedia(nodeName,
                new AvatarImage[] {avatar},
                mediaFiles -> this.saveAvatar(nodeName, mediaFiles[0], avatar != null ? avatar.getShape() : null));
    }

    private void saveAvatar(String nodeName, MediaFile mediaFile, String shape) {
        if (mediaFile == null) {
            return;
        }
        try {
            Transaction.execute(txManager, () -> {
                subscriberRepository.updateRemoteAvatar(universalContext.nodeId(), nodeName, mediaFile, shape);
                userSubscriptionRepository.updateRemoteAvatar(universalContext.nodeId(), nodeName, mediaFile, shape);
                contactRepository.updateRemoteAvatar(universalContext.nodeId(), nodeName, mediaFile, shape);
                return null;
            });
            universalContext.send(new RemoteNodeAvatarChangedLiberin(nodeName, new AvatarImage(mediaFile, shape)));
        } catch (Throwable e) {
            log.error("Error saving the downloaded avatar: {}", e.getMessage());
        }
    }

}
