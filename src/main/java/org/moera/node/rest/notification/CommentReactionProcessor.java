package org.moera.node.rest.notification;

import jakarta.inject.Inject;

import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.model.RemoteCommentReactionDeletedAllLiberin;
import org.moera.node.liberin.model.RemoteCommentReactionDeletedLiberin;
import org.moera.node.model.notification.CommentReactionAddedNotification;
import org.moera.node.model.notification.CommentReactionDeletedAllNotification;
import org.moera.node.model.notification.CommentReactionDeletedNotification;
import org.moera.node.model.notification.NotificationType;
import org.moera.node.notification.receive.NotificationMapping;
import org.moera.node.notification.receive.NotificationProcessor;
import org.moera.node.task.Jobs;

@NotificationProcessor
public class CommentReactionProcessor {

    @Inject
    private UniversalContext universalContext;

    @Inject
    private Jobs jobs;

    @NotificationMapping(NotificationType.COMMENT_REACTION_ADDED)
    public void added(CommentReactionAddedNotification notification) {
        jobs.run(
                CommentReactionAddedJob.class,
                new CommentReactionAddedJob.Parameters(
                        notification.getSenderNodeName(),
                        notification.getPostingId(),
                        notification.getPostingNodeName(),
                        notification.getPostingFullName(),
                        notification.getPostingGender(),
                        notification.getPostingAvatar(),
                        notification.getPostingHeading(),
                        notification.getCommentId(),
                        notification.getCommentHeading(),
                        notification.getOwnerName(),
                        notification.getOwnerFullName(),
                        notification.getOwnerGender(),
                        notification.getOwnerAvatar(),
                        notification.isNegative(),
                        notification.getEmoji()),
                universalContext.nodeId());
    }

    @NotificationMapping(NotificationType.COMMENT_REACTION_DELETED)
    public void deleted(CommentReactionDeletedNotification notification) {
        universalContext.send(new RemoteCommentReactionDeletedLiberin(notification.getSenderNodeName(),
                notification.getPostingId(), notification.getCommentId(), notification.getOwnerName(),
                notification.isNegative()));
    }

    @NotificationMapping(NotificationType.COMMENT_REACTION_DELETED_ALL)
    public void deletedAll(CommentReactionDeletedAllNotification notification) {
        universalContext.send(new RemoteCommentReactionDeletedAllLiberin(notification.getSenderNodeName(),
                notification.getPostingId(), notification.getCommentId()));
    }

}
