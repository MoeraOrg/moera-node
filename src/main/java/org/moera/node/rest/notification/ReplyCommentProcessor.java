package org.moera.node.rest.notification;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.moera.node.instant.ReplyCommentInstants;
import org.moera.node.model.notification.NotificationType;
import org.moera.node.model.notification.ReplyCommentAddedNotification;
import org.moera.node.model.notification.ReplyCommentDeletedNotification;
import org.moera.node.notification.receive.NotificationMapping;
import org.moera.node.notification.receive.NotificationProcessor;

@NotificationProcessor
public class ReplyCommentProcessor {

    @Inject
    private ReplyCommentInstants replyCommentInstants;

    @NotificationMapping(NotificationType.REPLY_COMMENT_ADDED)
    @Transactional
    public void added(ReplyCommentAddedNotification notification) {
        replyCommentInstants.added(notification.getSenderNodeName(), notification.getPostingId(),
                notification.getCommentId(), notification.getRepliedToId(), notification.getCommentOwnerName(),
                notification.getPostingHeading(), notification.getRepliedToHeading());
    }

    @NotificationMapping(NotificationType.REPLY_COMMENT_DELETED)
    @Transactional
    public void deleted(ReplyCommentDeletedNotification notification) {
        replyCommentInstants.deleted(notification.getSenderNodeName(), notification.getPostingId(),
                notification.getCommentId(), notification.getCommentOwnerName());
    }

}
