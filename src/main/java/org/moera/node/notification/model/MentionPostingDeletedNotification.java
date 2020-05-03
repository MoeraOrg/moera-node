package org.moera.node.notification.model;

import java.util.UUID;

import org.moera.node.notification.NotificationType;

public class MentionPostingDeletedNotification extends MentionPostingNotification {

    public MentionPostingDeletedNotification() {
        super(NotificationType.MENTION_POSTING_DELETED);
    }

    public MentionPostingDeletedNotification(UUID postingId) {
        super(NotificationType.MENTION_POSTING_DELETED, postingId);
    }
}
