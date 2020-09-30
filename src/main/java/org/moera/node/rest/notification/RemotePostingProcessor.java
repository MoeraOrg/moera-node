package org.moera.node.rest.notification;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.moera.node.data.Subscription;
import org.moera.node.data.SubscriptionRepository;
import org.moera.node.data.SubscriptionType;
import org.moera.node.global.RequestContext;
import org.moera.node.instant.RemoteCommentInstants;
import org.moera.node.model.UnsubscribeFailure;
import org.moera.node.model.notification.NotificationType;
import org.moera.node.model.notification.PostingCommentAddedNotification;
import org.moera.node.model.notification.PostingCommentDeletedNotification;
import org.moera.node.model.notification.PostingCommentNotification;
import org.moera.node.notification.receive.NotificationMapping;
import org.moera.node.notification.receive.NotificationProcessor;

@NotificationProcessor
public class RemotePostingProcessor {

    @Inject
    private RequestContext requestContext;

    @Inject
    private SubscriptionRepository subscriptionRepository;

    @Inject
    private RemoteCommentInstants remoteCommentInstants;

    private Subscription getSubscription(PostingCommentNotification notification) {
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
        remoteCommentInstants.added(
                notification.getSenderNodeName(), notification.getPostingId(), notification.getPostingHeading(),
                notification.getCommentOwnerName(), notification.getCommentId(), notification.getCommentHeading(),
                subscription.getReason());
    }

    @NotificationMapping(NotificationType.POSTING_COMMENT_DELETED)
    @Transactional
    public void commentDeleted(PostingCommentDeletedNotification notification) {
        Subscription subscription = getSubscription(notification);
        remoteCommentInstants.deleted(notification.getSenderNodeName(), notification.getPostingId(),
                notification.getCommentOwnerName(), notification.getCommentId(), subscription.getReason());
    }

}
