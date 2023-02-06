package org.moera.node.rest.notification;

import javax.inject.Inject;

import org.moera.node.data.MediaFile;
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
import org.moera.node.operations.ContactOperations;

@NotificationProcessor
public class ProfileProcessor {

    @Inject
    private UniversalContext universalContext;

    @Inject
    private SubscriptionRepository subscriptionRepository;

    @Inject
    private ContactOperations contactOperations;

    @Inject
    private MediaManager mediaManager;

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
        contactOperations.updateDetails(nodeName, fullName, gender);
        universalContext.send(new RemoteNodeFullNameChangedLiberin(nodeName, fullName));

        mediaManager.asyncDownloadPublicMedia(nodeName,
                new AvatarImage[] {avatar},
                () -> {
                    if (avatar != null && avatar.getMediaFile() != null) {
                        this.saveAvatar(nodeName, avatar.getMediaFile(), avatar.getShape());
                    }
                });
    }

    private void saveAvatar(String nodeName, MediaFile mediaFile, String shape) {
        contactOperations.updateAvatar(nodeName, mediaFile, shape);
        universalContext.send(new RemoteNodeAvatarChangedLiberin(nodeName, new AvatarImage(mediaFile, shape)));
    }

}
