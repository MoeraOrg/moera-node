package org.moera.node.rest.notification;

import jakarta.inject.Inject;

import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.model.ReplyCommentDeletedLiberin;
import org.moera.node.model.notification.NotificationType;
import org.moera.node.model.notification.ReplyCommentAddedNotification;
import org.moera.node.model.notification.ReplyCommentDeletedNotification;
import org.moera.node.notification.receive.NotificationMapping;
import org.moera.node.notification.receive.NotificationProcessor;
import org.moera.node.task.Jobs;

@NotificationProcessor
public class ReplyCommentProcessor {

    @Inject
    private UniversalContext universalContext;

    @Inject
    private Jobs jobs;

    @NotificationMapping(NotificationType.REPLY_COMMENT_ADDED)
    public void added(ReplyCommentAddedNotification notification) {
        jobs.run(
                ReplyCommentAddedJob.class,
                new ReplyCommentAddedJob.Parameters(
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
                        notification.getRepliedToId(),
                        notification.getRepliedToHeading(),
                        notification.getCommentOwnerName(),
                        notification.getCommentOwnerFullName(),
                        notification.getCommentOwnerGender(),
                        notification.getCommentOwnerAvatar(),
                        notification.getCommentSheriffMarks()),
                universalContext.nodeId());
    }

    @NotificationMapping(NotificationType.REPLY_COMMENT_DELETED)
    public void deleted(ReplyCommentDeletedNotification notification) {
        universalContext.send(
                new ReplyCommentDeletedLiberin(notification.getSenderNodeName(), notification.getPostingId(),
                        notification.getCommentId(), notification.getCommentOwnerName()));
    }

}
