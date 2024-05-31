package org.moera.node.rest.notification;

import javax.inject.Inject;

import org.moera.node.global.UniversalContext;
import org.moera.node.model.notification.NotificationType;
import org.moera.node.model.notification.SheriffComplaintDecidedNotification;
import org.moera.node.model.notification.SheriffOrderForCommentAddedNotification;
import org.moera.node.model.notification.SheriffOrderForCommentDeletedNotification;
import org.moera.node.model.notification.SheriffOrderForPostingAddedNotification;
import org.moera.node.model.notification.SheriffOrderForPostingDeletedNotification;
import org.moera.node.notification.receive.NotificationMapping;
import org.moera.node.notification.receive.NotificationProcessor;
import org.moera.node.task.Jobs;

@NotificationProcessor
public class SheriffProcessor {

    @Inject
    private UniversalContext universalContext;

    @Inject
    private Jobs jobs;

    @NotificationMapping(NotificationType.SHERIFF_ORDER_FOR_POSTING_ADDED)
    public void orderForPostingAdded(SheriffOrderForPostingAddedNotification notification) {
        jobs.run(
                SheriffOrderForPostingReceivedJob.class,
                new SheriffOrderForPostingReceivedJob.Parameters(false, notification),
                universalContext.nodeId());
    }

    @NotificationMapping(NotificationType.SHERIFF_ORDER_FOR_POSTING_DELETED)
    public void orderForPostingDeleted(SheriffOrderForPostingDeletedNotification notification) {
        jobs.run(
                SheriffOrderForPostingReceivedJob.class,
                new SheriffOrderForPostingReceivedJob.Parameters(true, notification),
                universalContext.nodeId());
    }

    @NotificationMapping(NotificationType.SHERIFF_ORDER_FOR_COMMENT_ADDED)
    public void orderForCommentAdded(SheriffOrderForCommentAddedNotification notification) {
        jobs.run(
                SheriffOrderForCommentReceivedJob.class,
                new SheriffOrderForCommentReceivedJob.Parameters(false, notification),
                universalContext.nodeId());
    }

    @NotificationMapping(NotificationType.SHERIFF_ORDER_FOR_COMMENT_DELETED)
    public void orderForCommentDeleted(SheriffOrderForCommentDeletedNotification notification) {
        jobs.run(
                SheriffOrderForCommentReceivedJob.class,
                new SheriffOrderForCommentReceivedJob.Parameters(true, notification),
                universalContext.nodeId());
    }

    @NotificationMapping(NotificationType.SHERIFF_COMPLAINT_DECIDED)
    public void complaintDecided(SheriffComplaintDecidedNotification notification) {
        jobs.run(
                SheriffComplaintDecidedJob.class,
                new SheriffComplaintDecidedJob.Parameters(notification),
                universalContext.nodeId());
    }

}
