package org.moera.node.rest.notification;

import jakarta.inject.Inject;

import org.moera.lib.node.types.notifications.NotificationType;
import org.moera.lib.node.types.notifications.SheriffComplaintDecidedNotification;
import org.moera.lib.node.types.notifications.SheriffOrderForCommentAddedNotification;
import org.moera.lib.node.types.notifications.SheriffOrderForCommentDeletedNotification;
import org.moera.lib.node.types.notifications.SheriffOrderForPostingAddedNotification;
import org.moera.lib.node.types.notifications.SheriffOrderForPostingDeletedNotification;
import org.moera.node.global.UniversalContext;
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
            new SheriffOrderForPostingReceivedJob.Parameters(notification),
            universalContext.nodeId()
        );
    }

    @NotificationMapping(NotificationType.SHERIFF_ORDER_FOR_POSTING_DELETED)
    public void orderForPostingDeleted(SheriffOrderForPostingDeletedNotification notification) {
        jobs.run(
            SheriffOrderForPostingReceivedJob.class,
            new SheriffOrderForPostingReceivedJob.Parameters(notification),
            universalContext.nodeId()
        );
    }

    @NotificationMapping(NotificationType.SHERIFF_ORDER_FOR_COMMENT_ADDED)
    public void orderForCommentAdded(SheriffOrderForCommentAddedNotification notification) {
        jobs.run(
            SheriffOrderForCommentReceivedJob.class,
            new SheriffOrderForCommentReceivedJob.Parameters(notification),
            universalContext.nodeId()
        );
    }

    @NotificationMapping(NotificationType.SHERIFF_ORDER_FOR_COMMENT_DELETED)
    public void orderForCommentDeleted(SheriffOrderForCommentDeletedNotification notification) {
        jobs.run(
            SheriffOrderForCommentReceivedJob.class,
            new SheriffOrderForCommentReceivedJob.Parameters(notification),
            universalContext.nodeId()
        );
    }

    @NotificationMapping(NotificationType.SHERIFF_COMPLAINT_DECIDED)
    public void complaintDecided(SheriffComplaintDecidedNotification notification) {
        jobs.run(
            SheriffComplaintDecidedJob.class,
            new SheriffComplaintDecidedJob.Parameters(notification),
            universalContext.nodeId()
        );
    }

}
