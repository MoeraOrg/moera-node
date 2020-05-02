package org.moera.node.notification.model;

import org.moera.node.notification.NotificationType;

public class MentionPostingDeletedNotification extends MentionPostingNotification {

    public MentionPostingDeletedNotification() {
        super(NotificationType.MENTION_POSTING_DELETED);
    }

}
