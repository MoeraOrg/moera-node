package org.moera.node.model.notification;

import java.util.UUID;

public class MentionPostingDeletedNotification extends MentionPostingNotification {

    public MentionPostingDeletedNotification() {
        super(NotificationType.MENTION_POSTING_DELETED);
    }

    public MentionPostingDeletedNotification(UUID postingId) {
        super(NotificationType.MENTION_POSTING_DELETED, postingId);
    }
}
