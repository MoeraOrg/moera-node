package org.moera.node.rest.notification;

import org.moera.node.notification.NotificationMapping;
import org.moera.node.notification.NotificationProcessor;
import org.moera.node.notification.NotificationType;
import org.moera.node.notification.model.MentionPostingAddedNotification;
import org.moera.node.notification.model.MentionPostingDeletedNotification;

@NotificationProcessor
public class MentionPostingProcessor {

    @NotificationMapping(NotificationType.MENTION_POSTING_ADDED)
    public void added(MentionPostingAddedNotification notification) {

    }

    @NotificationMapping(NotificationType.MENTION_POSTING_DELETED)
    public void added(MentionPostingDeletedNotification notification) {

    }

}
