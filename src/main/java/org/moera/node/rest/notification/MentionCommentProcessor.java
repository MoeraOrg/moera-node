package org.moera.node.rest.notification;

import jakarta.inject.Inject;

import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.model.MentionInRemoteCommentDeletedLiberin;
import org.moera.node.model.notification.MentionCommentAddedNotification;
import org.moera.node.model.notification.MentionCommentDeletedNotification;
import org.moera.node.model.notification.NotificationType;
import org.moera.node.notification.receive.NotificationMapping;
import org.moera.node.notification.receive.NotificationProcessor;
import org.moera.node.task.Jobs;

@NotificationProcessor
public class MentionCommentProcessor {

    @Inject
    private UniversalContext universalContext;

    @Inject
    private Jobs jobs;

    @NotificationMapping(NotificationType.MENTION_COMMENT_ADDED)
    public void added(MentionCommentAddedNotification notification) {
        jobs.run(
                MentionCommentAddedJob.class,
                new MentionCommentAddedJob.Parameters(
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
                        notification.getCommentSheriffMarks()),
                universalContext.nodeId());
    }

    @NotificationMapping(NotificationType.MENTION_COMMENT_DELETED)
    public void deleted(MentionCommentDeletedNotification notification) {
        universalContext.send(
                new MentionInRemoteCommentDeletedLiberin(notification.getSenderNodeName(), notification.getPostingId(),
                        notification.getCommentId()));
    }

}
