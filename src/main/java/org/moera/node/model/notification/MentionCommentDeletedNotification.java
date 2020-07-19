package org.moera.node.model.notification;

import java.util.UUID;

public class MentionCommentDeletedNotification extends MentionCommentNotification {

    public MentionCommentDeletedNotification() {
        super(NotificationType.MENTION_COMMENT_DELETED);
    }

    public MentionCommentDeletedNotification(UUID postingId, UUID commentId) {
        super(NotificationType.MENTION_COMMENT_DELETED, postingId, commentId);
    }

}
