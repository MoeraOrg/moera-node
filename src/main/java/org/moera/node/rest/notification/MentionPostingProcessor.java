package org.moera.node.rest.notification;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.moera.node.data.SubscriptionReason;
import org.moera.node.instant.MentionPostingInstants;
import org.moera.node.model.notification.MentionPostingAddedNotification;
import org.moera.node.model.notification.MentionPostingDeletedNotification;
import org.moera.node.model.notification.NotificationType;
import org.moera.node.notification.receive.NotificationMapping;
import org.moera.node.notification.receive.NotificationProcessor;
import org.moera.node.rest.task.RemotePostingCommentsSubscribeTask;
import org.moera.node.task.TaskAutowire;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;

@NotificationProcessor
public class MentionPostingProcessor {

    @Inject
    private MentionPostingInstants mentionPostingInstants;

    @Inject
    private TaskAutowire taskAutowire;

    @Inject
    @Qualifier("remoteTaskExecutor")
    private TaskExecutor taskExecutor;

    @NotificationMapping(NotificationType.MENTION_POSTING_ADDED)
    @Transactional
    public void added(MentionPostingAddedNotification notification) {
        mentionPostingInstants.added(notification.getSenderNodeName(), notification.getSenderFullName(),
                notification.getPostingId(), notification.getHeading());
        var task = new RemotePostingCommentsSubscribeTask(
                notification.getSenderNodeName(), notification.getPostingId(), SubscriptionReason.MENTION);
        taskAutowire.autowire(task);
        taskExecutor.execute(task);
    }

    @NotificationMapping(NotificationType.MENTION_POSTING_DELETED)
    @Transactional
    public void deleted(MentionPostingDeletedNotification notification) {
        mentionPostingInstants.deleted(notification.getSenderNodeName(), notification.getPostingId());
    }

}
