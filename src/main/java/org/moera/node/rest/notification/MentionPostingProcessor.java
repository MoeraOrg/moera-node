package org.moera.node.rest.notification;

import javax.inject.Inject;

import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.model.MentionInRemotePostingDeletedLiberin;
import org.moera.node.model.notification.MentionPostingAddedNotification;
import org.moera.node.model.notification.MentionPostingDeletedNotification;
import org.moera.node.model.notification.NotificationType;
import org.moera.node.notification.receive.NotificationMapping;
import org.moera.node.notification.receive.NotificationProcessor;
import org.moera.node.task.Jobs;

@NotificationProcessor
public class MentionPostingProcessor {

    @Inject
    private UniversalContext universalContext;

    @Inject
    private Jobs jobs;

    @NotificationMapping(NotificationType.MENTION_POSTING_ADDED)
    public void added(MentionPostingAddedNotification notification) {
        jobs.run(
                MentionPostingAddedJob.class,
                new MentionPostingAddedJob.Parameters(
                        notification.getSenderNodeName(),
                        notification.getPostingId(),
                        notification.getOwnerName(),
                        notification.getOwnerFullName(),
                        notification.getOwnerGender(),
                        notification.getOwnerAvatar(),
                        notification.getHeading(),
                        notification.getSheriffs(),
                        notification.getSheriffMarks()),
                universalContext.nodeId());
    }

    @NotificationMapping(NotificationType.MENTION_POSTING_DELETED)
    public void deleted(MentionPostingDeletedNotification notification) {
        universalContext.send(
                new MentionInRemotePostingDeletedLiberin(notification.getSenderNodeName(), notification.getPostingId()));
    }

}
