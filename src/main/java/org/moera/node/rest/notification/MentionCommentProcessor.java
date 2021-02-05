package org.moera.node.rest.notification;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.moera.node.instant.MentionCommentInstants;
import org.moera.node.model.notification.MentionCommentAddedNotification;
import org.moera.node.model.notification.MentionCommentDeletedNotification;
import org.moera.node.model.notification.NotificationType;
import org.moera.node.notification.receive.NotificationMapping;
import org.moera.node.notification.receive.NotificationProcessor;

@NotificationProcessor
public class MentionCommentProcessor {

    @Inject
    private MentionCommentInstants mentionCommentInstants;

    @NotificationMapping(NotificationType.MENTION_COMMENT_ADDED)
    @Transactional
    public void added(MentionCommentAddedNotification notification) {
        mentionCommentInstants.added(notification.getSenderNodeName(), notification.getSenderFullName(),
                notification.getPostingId(), notification.getPostingHeading(), notification.getCommentOwnerName(),
                notification.getCommentId(), notification.getCommentHeading());
    }

    @NotificationMapping(NotificationType.MENTION_COMMENT_DELETED)
    @Transactional
    public void deleted(MentionCommentDeletedNotification notification) {
        mentionCommentInstants.deleted(notification.getSenderNodeName(), notification.getPostingId(),
                notification.getCommentId());
    }

}
