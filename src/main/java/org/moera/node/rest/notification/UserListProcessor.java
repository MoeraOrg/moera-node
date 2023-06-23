package org.moera.node.rest.notification;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.moera.node.data.Subscription;
import org.moera.node.data.SubscriptionRepository;
import org.moera.node.data.SubscriptionType;
import org.moera.node.global.RequestContext;
import org.moera.node.model.UnsubscribeFailure;
import org.moera.node.model.notification.NotificationType;
import org.moera.node.model.notification.UserListItemAddedNotification;
import org.moera.node.model.notification.UserListItemDeletedNotification;
import org.moera.node.model.notification.UserListItemNotification;
import org.moera.node.notification.receive.NotificationMapping;
import org.moera.node.notification.receive.NotificationProcessor;
import org.moera.node.operations.FeedOperations;
import org.moera.node.operations.UserListOperations;
import org.moera.node.rest.task.UserListUpdateTask;
import org.moera.node.task.TaskAutowire;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;

@NotificationProcessor
public class UserListProcessor {

    @Inject
    private RequestContext requestContext;

    @Inject
    private SubscriptionRepository subscriptionRepository;

    @Inject
    private UserListOperations userListOperations;

    @Inject
    private FeedOperations feedOperations;

    @Inject
    @Qualifier("remoteTaskExecutor")
    private TaskExecutor taskExecutor;

    @Inject
    private TaskAutowire taskAutowire;

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
                requestContext.nodeId(), notification.getSenderNodeName(), notification.getSubscriberId()).orElse(null);
        if (subscription == null || subscription.getSubscriptionType() != SubscriptionType.USER_LIST
                || !notification.getListName().equals(subscription.getRemoteFeedName())) {
            throw new UnsubscribeFailure();
        }

        var updateTask = new UserListUpdateTask(
                notification.getSenderNodeName(),
                notification.getListName(),
                feedOperations.getSheriffFeeds(notification.getSenderNodeName()),
                notification.getNodeName(),
                delete);
        taskAutowire.autowire(updateTask);
        taskExecutor.execute(updateTask);
    }

}
