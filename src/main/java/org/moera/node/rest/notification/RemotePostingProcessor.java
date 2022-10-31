package org.moera.node.rest.notification;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.moera.node.data.OwnCommentRepository;
import org.moera.node.data.Subscription;
import org.moera.node.data.SubscriptionReason;
import org.moera.node.data.SubscriptionRepository;
import org.moera.node.data.SubscriptionType;
import org.moera.node.data.UserSubscription;
import org.moera.node.data.UserSubscriptionRepository;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.model.ForeignCommentAddedLiberin;
import org.moera.node.liberin.model.ForeignCommentDeletedLiberin;
import org.moera.node.liberin.model.RemotePostingImportantUpdateLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.model.AvatarImage;
import org.moera.node.model.UnsubscribeFailure;
import org.moera.node.model.notification.NotificationType;
import org.moera.node.model.notification.PostingCommentAddedNotification;
import org.moera.node.model.notification.PostingCommentDeletedNotification;
import org.moera.node.model.notification.PostingImportantUpdateNotification;
import org.moera.node.model.notification.PostingSubscriberNotification;
import org.moera.node.notification.receive.NotificationMapping;
import org.moera.node.notification.receive.NotificationProcessor;

@NotificationProcessor
public class RemotePostingProcessor {

    @Inject
    private UniversalContext universalContext;

    @Inject
    private SubscriptionRepository subscriptionRepository;

    @Inject
    private UserSubscriptionRepository userSubscriptionRepository;

    @Inject
    private OwnCommentRepository ownCommentRepository;

    @Inject
    private MediaManager mediaManager;

    private Subscription getSubscription(PostingSubscriberNotification notification) {
        Subscription subscription = subscriptionRepository.findBySubscriber(
                universalContext.nodeId(), notification.getSenderNodeName(), notification.getSubscriberId()).orElse(null);
        if (subscription == null || subscription.getSubscriptionType() != SubscriptionType.POSTING_COMMENTS
                || !notification.getPostingId().equals(subscription.getRemoteEntryId())) {
            throw new UnsubscribeFailure();
        }
        return subscription;
    }

    private SubscriptionReason getSubscriptionReason(Subscription subscription) {
        return userSubscriptionRepository.findAllByTypeAndNodeAndEntryId(
                        subscription.getNodeId(), subscription.getSubscriptionType(), subscription.getRemoteNodeName(),
                        subscription.getRemoteEntryId()).stream()
                .map(UserSubscription::getReason)
                .findFirst()
                .orElse(SubscriptionReason.USER);
    }

    @NotificationMapping(NotificationType.POSTING_COMMENT_ADDED)
    @Transactional
    public void commentAdded(PostingCommentAddedNotification notification) {
        SubscriptionReason reason = getSubscriptionReason(getSubscription(notification));
        if (notification.getCommentRepliedTo() != null) {
            int count = ownCommentRepository.countByRemoteCommentId(universalContext.nodeId(),
                    notification.getSenderNodeName(), notification.getPostingId(), notification.getCommentRepliedTo());
            if (count > 0) {
                return; // We should receive another notification about somebody replied to our comment
            }
        }
        mediaManager.asyncDownloadPublicMedia(notification.getSenderNodeName(),
                new AvatarImage[] {notification.getPostingOwnerAvatar(), notification.getCommentOwnerAvatar()},
                mediaFiles -> {
                    if (notification.getPostingOwnerAvatar() != null) {
                        notification.getPostingOwnerAvatar().setMediaFile(mediaFiles[0]);
                    }
                    if (notification.getCommentOwnerAvatar() != null) {
                        notification.getCommentOwnerAvatar().setMediaFile(mediaFiles[1]);
                    }
                    universalContext.send(
                            new ForeignCommentAddedLiberin(notification.getSenderNodeName(),
                                    notification.getPostingOwnerName(), notification.getPostingOwnerFullName(),
                                    notification.getPostingOwnerGender(), notification.getPostingOwnerAvatar(),
                                    notification.getPostingId(), notification.getPostingHeading(),
                                    notification.getCommentOwnerName(), notification.getCommentOwnerFullName(),
                                    notification.getCommentOwnerGender(), notification.getCommentOwnerAvatar(),
                                    notification.getCommentId(), notification.getCommentHeading(), reason));
                });
    }

    @NotificationMapping(NotificationType.POSTING_COMMENT_DELETED)
    @Transactional
    public void commentDeleted(PostingCommentDeletedNotification notification) {
        SubscriptionReason reason = getSubscriptionReason(getSubscription(notification));
        universalContext.send(
                new ForeignCommentDeletedLiberin(notification.getSenderNodeName(), notification.getPostingId(),
                        notification.getCommentOwnerName(), notification.getCommentId(), reason));
    }

    @NotificationMapping(NotificationType.POSTING_IMPORTANT_UPDATE)
    @Transactional
    public void postingUpdated(PostingImportantUpdateNotification notification) {
        getSubscription(notification);
        mediaManager.asyncDownloadPublicMedia(notification.getSenderNodeName(),
                new AvatarImage[] {notification.getPostingOwnerAvatar()},
                mediaFiles -> {
                    if (notification.getPostingOwnerAvatar() != null) {
                        notification.getPostingOwnerAvatar().setMediaFile(mediaFiles[0]);
                    }
                    universalContext.send(
                            new RemotePostingImportantUpdateLiberin(notification.getSenderNodeName(),
                                    notification.getPostingOwnerName(), notification.getPostingOwnerFullName(),
                                    notification.getPostingOwnerGender(), notification.getPostingOwnerAvatar(),
                                    notification.getPostingId(), notification.getPostingHeading(),
                                    notification.getDescription()));
                });
    }

}
