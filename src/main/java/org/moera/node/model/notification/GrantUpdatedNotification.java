package org.moera.node.model.notification;

public class GrantUpdatedNotification extends Notification {

    private long scope;

    public GrantUpdatedNotification() {
        super(NotificationType.GRANT_UPDATED);
    }

    public GrantUpdatedNotification(long scope) {
        super(NotificationType.GRANT_UPDATED);
        this.scope = scope;
    }

    public long getScope() {
        return scope;
    }

    public void setScope(long scope) {
        this.scope = scope;
    }

}
