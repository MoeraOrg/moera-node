package org.moera.node.notification;

import org.moera.node.notification.model.MentionPostingAddedNotification;
import org.moera.node.notification.model.MentionPostingDeletedNotification;
import org.moera.node.notification.model.Notification;

public enum NotificationType {

    MENTION_POSTING_ADDED(MentionPostingAddedNotification.class),
    MENTION_POSTING_DELETED(MentionPostingDeletedNotification.class);

    private Class<? extends Notification> structure;

    NotificationType(Class<? extends Notification> structure) {
        this.structure = structure;
    }

    public Class<? extends Notification> getStructure() {
        return structure;
    }

    public String getValue() {
        return name().toLowerCase().replace('_', '-');
    }

    public static NotificationType forValue(String value) {
        String name = value.toUpperCase().replace('-', '_');
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
