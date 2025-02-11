package org.moera.node.rest.notification;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.moera.lib.node.types.SubscriptionType;
import org.moera.node.data.Subscription;
import org.moera.node.data.SubscriptionRepository;
import org.moera.node.global.UniversalContext;
import org.moera.node.model.UnsubscribeFailure;
import org.moera.node.model.notification.NotificationType;
import org.moera.node.model.notification.UserListItemAddedNotification;
import org.moera.node.model.notification.UserListItemDeletedNotification;
import org.moera.node.model.notification.UserListItemNotification;
import org.moera.node.notification.receive.NotificationMapping;
import org.moera.node.notification.receive.NotificationProcessor;
import org.moera.node.operations.FeedOperations;
import org.moera.node.rest.task.UserListUpdateJob;
import org.moera.node.task.Jobs;

@NotificationProcessor
public class UserListProcessor {

    @Inject
    private UniversalContext universalContext;

    @Inject
    private SubscriptionRepository subscriptionRepository;

    @Inject
    private FeedOperations feedOperations;

    @Inject
    private Jobs jobs;

    @NotificationMapping(NotificationType.USER_LIST_ITEM_ADDED)
    @Transactional
    public void added(UserListItemAddedNotification notification) {
        updated(notification, false);
    }

    @NotificationMapping(NotificationType.USER_LIST_ITEM_DELETED)
    @Transactional
    public void deleted(UserListItemDeletedNotification notification) {
        updated(notification, true);
    }

    private void updated(UserListItemNotification notification, boolean delete) {
        Subscription subscription = subscriptionRepository.findBySubscriber(
                    universalContext.nodeId(), notification.getSenderNodeName(), notification.getSubscriberId())
                .orElse(null);
        if (subscription == null
                || subscription.getSubscriptionType() != SubscriptionType.USER_LIST
                || !notification.getListName().equals(subscription.getRemoteFeedName())) {
            throw new UnsubscribeFailure();
        }

        jobs.run(
                UserListUpdateJob.class,
                new UserListUpdateJob.Parameters(
                        notification.getSenderNodeName(),
                        notification.getListName(),
                        feedOperations.getSheriffFeeds(notification.getSenderNodeName()),
                        notification.getNodeName(),
                        delete),
                universalContext.nodeId());
    }

}
