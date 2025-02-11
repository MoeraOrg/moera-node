package org.moera.node.model.notification;

import java.util.List;

import org.moera.lib.node.types.Scope;

public class GrantUpdatedNotification extends Notification {

    private List<String> scope;

    public GrantUpdatedNotification() {
        super(NotificationType.GRANT_UPDATED);
    }

    public GrantUpdatedNotification(long scope) {
        super(NotificationType.GRANT_UPDATED);
        this.scope = Scope.toValues(scope);
    }

    public List<String> getScope() {
        return scope;
    }

    public void setScope(List<String> scope) {
        this.scope = scope;
    }

}
