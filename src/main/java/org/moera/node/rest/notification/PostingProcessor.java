package org.moera.node.rest.notification;

import javax.inject.Inject;

import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.Subscription;
import org.moera.node.data.SubscriptionRepository;
import org.moera.node.data.SubscriptionType;
import org.moera.node.global.RequestContext;
import org.moera.node.model.UnsubscribeFailure;
import org.moera.node.model.notification.FeedPostingAddedNotification;
import org.moera.node.model.notification.NotificationType;
import org.moera.node.model.notification.PostingUpdatedNotification;
import org.moera.node.notification.receive.NotificationMapping;
import org.moera.node.notification.receive.NotificationProcessor;
import org.moera.node.picker.Pick;
import org.moera.node.picker.PickerPool;

@NotificationProcessor
public class PostingProcessor {

    @Inject
    private RequestContext requestContext;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private SubscriptionRepository subscriptionRepository;

    @Inject
    private PickerPool pickerPool;

    @NotificationMapping(NotificationType.FEED_POSTING_ADDED)
    public void added(FeedPostingAddedNotification notification) {
        Subscription subscription = subscriptionRepository.findBySubscriber(
                requestContext.nodeId(), SubscriptionType.FEED, notification.getSenderNodeName(),
                notification.getSubscriberId()).orElse(null);
        if (subscription == null || !notification.getFeedName().equals(subscription.getRemoteFeedName())) {
            throw new UnsubscribeFailure();
        }

        Pick pick = new Pick();
        pick.setRemoteNodeName(subscription.getRemoteNodeName());
        pick.setRemoteFeedName(subscription.getRemoteFeedName());
        pick.setRemotePostingId(notification.getPostingId());
        pick.setFeedName(subscription.getFeedName());
        pickerPool.pick(pick);
    }

    @NotificationMapping(NotificationType.POSTING_UPDATED)
    public void updated(PostingUpdatedNotification notification) {
        Subscription subscription = subscriptionRepository.findBySubscriber(
                requestContext.nodeId(), SubscriptionType.POSTING, notification.getSenderNodeName(),
                notification.getSubscriberId()).orElse(null);
        if (subscription == null || !notification.getPostingId().equals(subscription.getRemoteEntryId())) {
            throw new UnsubscribeFailure();
        }
        Posting posting = postingRepository.findByReceiverId(requestContext.nodeId(), subscription.getRemoteNodeName(),
                notification.getPostingId()).orElse(null);
        if (posting == null) {
            throw new UnsubscribeFailure();
        }

        Pick pick = new Pick();
        pick.setRemoteNodeName(subscription.getRemoteNodeName());
        pick.setRemotePostingId(notification.getPostingId());
        pickerPool.pick(pick);
    }

}
