package org.moera.node.notification.model;

import org.moera.node.notification.NotificationType;

public class MentionPostingAddedNotification extends MentionPostingNotification {

    private String heading;

    public MentionPostingAddedNotification() {
        super(NotificationType.MENTION_POSTING_ADDED);
    }

    public String getHeading() {
        return heading;
    }

    public void setHeading(String heading) {
        this.heading = heading;
    }

}
