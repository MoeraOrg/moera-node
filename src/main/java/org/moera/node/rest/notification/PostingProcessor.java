package org.moera.node.rest.notification;

import java.util.List;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.moera.lib.util.LogUtil;
import org.moera.node.data.Pick;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.ReactionTotalRepository;
import org.moera.node.data.StoryType;
import org.moera.node.data.Subscription;
import org.moera.node.data.SubscriptionRepository;
import org.moera.node.data.SubscriptionType;
import org.moera.node.data.UserSubscription;
import org.moera.node.data.UserSubscriptionRepository;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.model.PostingCommentTotalsUpdatedLiberin;
import org.moera.node.liberin.model.PostingReactionTotalsUpdatedLiberin;
import org.moera.node.model.UnsubscribeFailure;
import org.moera.node.model.notification.NotificationType;
import org.moera.node.model.notification.PostingCommentsUpdatedNotification;
import org.moera.node.model.notification.PostingDeletedNotification;
import org.moera.node.model.notification.PostingReactionsUpdatedNotification;
import org.moera.node.model.notification.PostingSubscriberNotification;
import org.moera.node.model.notification.PostingUpdatedNotification;
import org.moera.node.model.notification.StoryAddedNotification;
import org.moera.node.notification.receive.NotificationMapping;
import org.moera.node.notification.receive.NotificationProcessor;
import org.moera.node.operations.PostingOperations;
import org.moera.node.operations.ReactionTotalOperations;
import org.moera.node.picker.PickerPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NotificationProcessor
public class PostingProcessor {

    private static final Logger log = LoggerFactory.getLogger(PostingProcessor.class);

    private interface PostingSubscriptionRunnable {

        void run(Subscription subscription, Posting posting);

    }

    @Inject
    private UniversalContext universalContext;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private SubscriptionRepository subscriptionRepository;

    @Inject
    private UserSubscriptionRepository userSubscriptionRepository;

    @Inject
    private ReactionTotalRepository reactionTotalRepository;

    @Inject
    private PickerPool pickerPool;

    @Inject
    private PostingOperations postingOperations;

    @Inject
    private ReactionTotalOperations reactionTotalOperations;

    @NotificationMapping(NotificationType.STORY_ADDED)
    @Transactional
    public void added(StoryAddedNotification notification) {
        Subscription subscription = subscriptionRepository.findBySubscriber(
                universalContext.nodeId(), notification.getSenderNodeName(), notification.getSubscriberId()).orElse(null);
        if (subscription == null || subscription.getSubscriptionType() != SubscriptionType.FEED
                || !notification.getFeedName().equals(subscription.getRemoteFeedName())) {
            throw new UnsubscribeFailure();
        }

        if (notification.getStoryType() != StoryType.POSTING_ADDED || notification.getPostingId() == null) {
            return;
        }

        List<UserSubscription> userSubscriptions = userSubscriptionRepository.findAllByTypeAndNodeAndFeedName(
                universalContext.nodeId(), SubscriptionType.FEED, notification.getSenderNodeName(),
                subscription.getRemoteFeedName());

        for (UserSubscription userSubscription : userSubscriptions) {
            Pick pick = new Pick();
            pick.setRemoteNodeName(userSubscription.getRemoteNodeName());
            pick.setRemoteFeedName(userSubscription.getRemoteFeedName());
            pick.setRemotePostingId(notification.getPostingId());
            pick.setFeedName(userSubscription.getFeedName());
            pickerPool.pick(pick);
        }
    }

    private void withValidPostingSubscription(PostingSubscriberNotification notification,
                                              PostingSubscriptionRunnable runnable) {
        Subscription subscription = subscriptionRepository.findBySubscriber(
                universalContext.nodeId(), notification.getSenderNodeName(), notification.getSubscriberId()).orElse(null);
        if (subscription == null || subscription.getSubscriptionType() != SubscriptionType.POSTING
                || !notification.getPostingId().equals(subscription.getRemoteEntryId())) {
            throw new UnsubscribeFailure();
        }
        Posting posting = postingRepository.findByReceiverId(universalContext.nodeId(), subscription.getRemoteNodeName(),
                notification.getPostingId()).orElseThrow(UnsubscribeFailure::new);

        runnable.run(subscription, posting);
    }

    @NotificationMapping(NotificationType.POSTING_UPDATED)
    @Transactional
    public void updated(PostingUpdatedNotification notification) {
        withValidPostingSubscription(notification, (subscription, posting) -> {
            Pick pick = new Pick();
            pick.setRemoteNodeName(subscription.getRemoteNodeName());
            pick.setRemotePostingId(notification.getPostingId());
            pickerPool.pick(pick);
        });
    }

    @NotificationMapping(NotificationType.POSTING_DELETED)
    @Transactional
    public void deleted(PostingDeletedNotification notification) {
        withValidPostingSubscription(notification, (subscription, posting) ->
            postingOperations.deletePickedPosting(posting)
        );
    }

    @NotificationMapping(NotificationType.POSTING_REACTIONS_UPDATED)
    @Transactional
    public void reactionsUpdated(PostingReactionsUpdatedNotification notification) {
        withValidPostingSubscription(notification, (subscription, posting) -> {
            var reactionTotals = reactionTotalRepository.findAllByEntryId(posting.getId());
            if (!reactionTotalOperations.isSame(reactionTotals, notification.getTotals())) {
                reactionTotalOperations.replaceAll(posting, notification.getTotals());
            }
            // Totals may be the same, but events were not sent yet, because reactions weren't signed
            universalContext.send(new PostingReactionTotalsUpdatedLiberin(posting, notification.getTotals()));
        });
    }

    @NotificationMapping(NotificationType.POSTING_COMMENTS_UPDATED)
    @Transactional
    public void commentsUpdated(PostingCommentsUpdatedNotification notification) {
        withValidPostingSubscription(notification, (subscription, posting) -> {
            if (posting.getTotalChildren() != notification.getTotal()) {
                log.debug("Total comments for posting {} = {}: updated from notification",
                        LogUtil.format(posting.getId()), LogUtil.format(notification.getTotal()));
                posting.setTotalChildren(notification.getTotal());
            }
            // The number may be the same, but events were not sent yet, because comments weren't signed
            universalContext.send(new PostingCommentTotalsUpdatedLiberin(posting, notification.getTotal()));
        });
    }

}
