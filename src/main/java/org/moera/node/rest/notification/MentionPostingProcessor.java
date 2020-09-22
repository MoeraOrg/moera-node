package org.moera.node.rest.notification;

import javax.inject.Inject;

import org.moera.node.instant.MentionPostingInstants;
import org.moera.node.model.notification.MentionPostingAddedNotification;
import org.moera.node.model.notification.MentionPostingDeletedNotification;
import org.moera.node.model.notification.NotificationType;
import org.moera.node.notification.receive.NotificationMapping;
import org.moera.node.notification.receive.NotificationProcessor;

@NotificationProcessor
public class MentionPostingProcessor {

    @Inject
    private MentionPostingInstants mentionPostingInstants;

    @NotificationMapping(NotificationType.MENTION_POSTING_ADDED)
    public void added(MentionPostingAddedNotification notification) {
        mentionPostingInstants.added(
                notification.getSenderNodeName(), notification.getPostingId(), notification.getHeading());
    }

    @NotificationMapping(NotificationType.MENTION_POSTING_DELETED)
    public void deleted(MentionPostingDeletedNotification notification) {
        mentionPostingInstants.deleted(notification.getSenderNodeName(), notification.getPostingId());
    }

}
