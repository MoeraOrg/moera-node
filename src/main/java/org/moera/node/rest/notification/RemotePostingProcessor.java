package org.moera.node.rest.notification;

import jakarta.inject.Inject;

import org.moera.node.data.Subscription;
import org.moera.node.data.SubscriptionReason;
import org.moera.node.data.SubscriptionRepository;
import org.moera.node.data.SubscriptionType;
import org.moera.node.data.UserSubscription;
import org.moera.node.data.UserSubscriptionRepository;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.model.ForeignCommentDeletedLiberin;
import org.moera.node.model.UnsubscribeFailure;
import org.moera.node.model.notification.NotificationType;
import org.moera.node.model.notification.PostingCommentAddedNotification;
import org.moera.node.model.notification.PostingCommentDeletedNotification;
import org.moera.node.model.notification.PostingImportantUpdateNotification;
import org.moera.node.model.notification.PostingSubscriberNotification;
import org.moera.node.notification.receive.NotificationMapping;
import org.moera.node.notification.receive.NotificationProcessor;
import org.moera.node.task.Jobs;
import org.moera.node.util.Transaction;

@NotificationProcessor
public class RemotePostingProcessor {

    @Inject
    private UniversalContext universalContext;

    @Inject
    private SubscriptionRepository subscriptionRepository;

    @Inject
    private UserSubscriptionRepository userSubscriptionRepository;

    @Inject
    private Transaction tx;

    @Inject
    private Jobs jobs;

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
    public void commentAdded(PostingCommentAddedNotification notification) {
        SubscriptionReason reason = tx.executeRead(() -> getSubscriptionReason(getSubscription(notification)));
        jobs.run(
                RemotePostingCommentAddedJob.class,
                new RemotePostingCommentAddedJob.Parameters(
                        notification.getSenderNodeName(),
                        notification.getPostingId(),
                        notification.getPostingOwnerName(),
                        notification.getPostingOwnerFullName(),
                        notification.getPostingOwnerGender(),
                        notification.getPostingOwnerAvatar(),
                        notification.getPostingHeading(),
                        notification.getPostingSheriffs(),
                        notification.getPostingSheriffMarks(),
                        notification.getCommentId(),
                        notification.getCommentOwnerName(),
                        notification.getCommentOwnerFullName(),
                        notification.getCommentOwnerGender(),
                        notification.getCommentOwnerAvatar(),
                        notification.getCommentHeading(),
                        notification.getCommentSheriffMarks(),
                        notification.getCommentRepliedTo(),
                        reason),
                universalContext.nodeId());
    }

    @NotificationMapping(NotificationType.POSTING_COMMENT_DELETED)
    public void commentDeleted(PostingCommentDeletedNotification notification) {
        SubscriptionReason reason = tx.executeRead(() -> getSubscriptionReason(getSubscription(notification)));
        universalContext.send(
                new ForeignCommentDeletedLiberin(notification.getSenderNodeName(), notification.getPostingId(),
                        notification.getCommentOwnerName(), notification.getCommentId(), reason));
    }

    @NotificationMapping(NotificationType.POSTING_IMPORTANT_UPDATE)
    public void postingUpdated(PostingImportantUpdateNotification notification) {
        tx.executeRead(() -> getSubscription(notification));
        jobs.run(
                RemotePostingImportantUpdateJob.class,
                new RemotePostingImportantUpdateJob.Parameters(
                        notification.getSenderNodeName(),
                        notification.getPostingId(),
                        notification.getPostingOwnerName(),
                        notification.getPostingOwnerFullName(),
                        notification.getPostingOwnerGender(),
                        notification.getPostingOwnerAvatar(),
                        notification.getPostingHeading(),
                        notification.getDescription()),
                universalContext.nodeId());
    }

}
