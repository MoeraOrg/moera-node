package org.moera.node.rest.notification;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.moera.node.data.OwnCommentRepository;
import org.moera.node.data.Subscription;
import org.moera.node.data.SubscriptionRepository;
import org.moera.node.data.SubscriptionType;
import org.moera.node.global.RequestContext;
import org.moera.node.instant.PostingInstants;
import org.moera.node.instant.RemoteCommentInstants;
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
    private RequestContext requestContext;

    @Inject
    private SubscriptionRepository subscriptionRepository;

    @Inject
    private OwnCommentRepository ownCommentRepository;

    @Inject
    private RemoteCommentInstants remoteCommentInstants;

    @Inject
    private PostingInstants postingInstants;

    @Inject
    private MediaManager mediaManager;

    private Subscription getSubscription(PostingSubscriberNotification notification) {
        Subscription subscription = subscriptionRepository.findBySubscriber(
                requestContext.nodeId(), notification.getSenderNodeName(), notification.getSubscriberId()).orElse(null);
        if (subscription == null || subscription.getSubscriptionType() != SubscriptionType.POSTING_COMMENTS
                || !notification.getPostingId().equals(subscription.getRemoteEntryId())) {
            throw new UnsubscribeFailure();
        }
        return subscription;
    }

    @NotificationMapping(NotificationType.POSTING_COMMENT_ADDED)
    @Transactional
    public void commentAdded(PostingCommentAddedNotification notification) {
        Subscription subscription = getSubscription(notification);
        if (notification.getCommentRepliedTo() != null) {
            int count = ownCommentRepository.countByRemoteCommentId(requestContext.nodeId(),
                    notification.getSenderNodeName(), notification.getPostingId(), notification.getCommentRepliedTo());
            if (count > 0) {
                return; // We should receive another notification about somebody replied to our comment
            }
        }
        mediaManager.asyncDownloadPublicMedia(notification.getSenderNodeName(),
                new AvatarImage[] {notification.getSenderAvatar(), notification.getCommentOwnerAvatar()},
                mediaFiles -> {
                    if (notification.getSenderAvatar() != null) {
                        notification.getSenderAvatar().setMediaFile(mediaFiles[0]);
                    }
                    if (notification.getCommentOwnerAvatar() != null) {
                        notification.getCommentOwnerAvatar().setMediaFile(mediaFiles[1]);
                    }
                    remoteCommentInstants.added(notification.getSenderNodeName(), notification.getSenderFullName(),
                            notification.getSenderAvatar(), notification.getPostingId(),
                            notification.getPostingHeading(), notification.getCommentOwnerName(),
                            notification.getCommentOwnerFullName(), notification.getCommentOwnerAvatar(),
                            notification.getCommentId(), notification.getCommentHeading(), subscription.getReason());
                });
    }

    @NotificationMapping(NotificationType.POSTING_COMMENT_DELETED)
    @Transactional
    public void commentDeleted(PostingCommentDeletedNotification notification) {
        Subscription subscription = getSubscription(notification);
        remoteCommentInstants.deleted(notification.getSenderNodeName(), notification.getPostingId(),
                notification.getCommentOwnerName(), notification.getCommentId(), subscription.getReason());
    }

    @NotificationMapping(NotificationType.POSTING_IMPORTANT_UPDATE)
    @Transactional
    public void postingUpdated(PostingImportantUpdateNotification notification) {
        getSubscription(notification);
        mediaManager.asyncDownloadPublicMedia(notification.getSenderNodeName(),
                new AvatarImage[] {notification.getSenderAvatar()},
                mediaFiles -> {
                    if (notification.getSenderAvatar() != null) {
                        notification.getSenderAvatar().setMediaFile(mediaFiles[0]);
                    }
                    postingInstants.updated(notification.getSenderNodeName(), notification.getSenderFullName(),
                            notification.getSenderAvatar(), notification.getPostingId(),
                            notification.getPostingHeading(), notification.getDescription());
                });
    }

}
