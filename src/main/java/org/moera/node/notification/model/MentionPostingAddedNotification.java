package org.moera.node.notification.model;

import java.util.UUID;

import org.moera.node.notification.NotificationType;

public class MentionPostingAddedNotification extends MentionPostingNotification {

    private String heading;

    public MentionPostingAddedNotification() {
        super(NotificationType.MENTION_POSTING_ADDED);
    }

    public MentionPostingAddedNotification(UUID postingId, String heading) {
        super(NotificationType.MENTION_POSTING_ADDED, postingId);
        this.heading = heading;
    }

    public String getHeading() {
        return heading;
    }

    public void setHeading(String heading) {
        this.heading = heading;
    }

}
