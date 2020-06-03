package org.moera.node.model.notification;

import java.util.UUID;

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
