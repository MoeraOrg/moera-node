package org.moera.node.rest.notification;

import jakarta.inject.Inject;

import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.model.RemoteCommentMediaReactionDeletedAllLiberin;
import org.moera.node.liberin.model.RemoteCommentMediaReactionDeletedLiberin;
import org.moera.node.liberin.model.RemotePostingMediaReactionDeletedAllLiberin;
import org.moera.node.liberin.model.RemotePostingMediaReactionDeletedLiberin;
import org.moera.node.model.notification.NotificationType;
import org.moera.node.model.notification.PostingReactionAddedNotification;
import org.moera.node.model.notification.PostingReactionDeletedAllNotification;
import org.moera.node.model.notification.PostingReactionDeletedNotification;
import org.moera.node.notification.receive.NotificationMapping;
import org.moera.node.notification.receive.NotificationProcessor;
import org.moera.node.task.Jobs;

@NotificationProcessor
public class PostingReactionProcessor {

    @Inject
    private UniversalContext universalContext;

    @Inject
    private Jobs jobs;

    @NotificationMapping(NotificationType.POSTING_REACTION_ADDED)
    public void added(PostingReactionAddedNotification notification) {
        jobs.run(
                PostingReactionAddedJob.class,
                new PostingReactionAddedJob.Parameters(
                        notification.getSenderNodeName(),
                        notification.getOwnerName(),
                        notification.getOwnerFullName(),
                        notification.getOwnerGender(),
                        notification.getOwnerAvatar(),
                        notification.isNegative(),
                        notification.getEmoji(),
                        notification.getParentPostingNodeName(),
                        notification.getParentPostingFullName(),
                        notification.getParentPostingGender(),
                        notification.getParentPostingAvatar(),
                        notification.getParentPostingId(),
                        notification.getParentHeading(),
                        notification.getParentCommentId(),
                        notification.getParentMediaId(),
                        notification.getPostingId()),
                universalContext.nodeId());
    }

    @NotificationMapping(NotificationType.POSTING_REACTION_DELETED)
    public void deleted(PostingReactionDeletedNotification notification) {
        if (notification.getParentPostingId() != null) {
            if (notification.getParentCommentId() == null) {
                universalContext.send(
                        new RemotePostingMediaReactionDeletedLiberin(notification.getSenderNodeName(),
                                notification.getPostingId(), notification.getOwnerName(), notification.isNegative()));
            } else {
                universalContext.send(
                        new RemoteCommentMediaReactionDeletedLiberin(notification.getSenderNodeName(),
                                notification.getPostingId(), notification.getOwnerName(), notification.isNegative()));
            }
        }
    }

    @NotificationMapping(NotificationType.POSTING_REACTION_DELETED_ALL)
    public void deletedAll(PostingReactionDeletedAllNotification notification) {
        if (notification.getParentPostingId() != null) {
            if (notification.getParentCommentId() == null) {
                universalContext.send(
                        new RemotePostingMediaReactionDeletedAllLiberin(notification.getSenderNodeName(),
                                notification.getPostingId()));
            } else {
                universalContext.send(
                        new RemoteCommentMediaReactionDeletedAllLiberin(notification.getSenderNodeName(),
                                notification.getPostingId()));
            }
        }
    }

}
