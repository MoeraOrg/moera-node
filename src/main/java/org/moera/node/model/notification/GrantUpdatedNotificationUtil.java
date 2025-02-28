package org.moera.node.model.notification;

import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.notifications.GrantUpdatedNotification;

public class GrantUpdatedNotificationUtil {
    
    public static GrantUpdatedNotification build(long scope) {
        GrantUpdatedNotification notification = new GrantUpdatedNotification();
        notification.setScope(Scope.toValues(scope));
        return notification;
    }

}
