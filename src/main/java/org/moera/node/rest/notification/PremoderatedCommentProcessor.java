package org.moera.node.rest.notification;

import jakarta.inject.Inject;

import org.moera.lib.node.types.notifications.NotificationType;
import org.moera.lib.node.types.notifications.PremoderatedCommentDecidedNotification;
import org.moera.node.global.UniversalContext;
import org.moera.node.notification.receive.NotificationMapping;
import org.moera.node.notification.receive.NotificationProcessor;
import org.moera.node.task.Jobs;

@NotificationProcessor
public class PremoderatedCommentProcessor {

    @Inject
    private UniversalContext universalContext;

    @Inject
    private Jobs jobs;

    @NotificationMapping(NotificationType.PREMODERATED_COMMENT_DECIDED)
    public void decided(PremoderatedCommentDecidedNotification notification) {
        jobs.run(
            PremoderatedCommentDecidedJob.class,
            new PremoderatedCommentDecidedJob.Parameters(notification),
            universalContext.nodeId()
        );
    }

}
