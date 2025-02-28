package org.moera.node.model.notification;

import org.moera.lib.node.types.AskDescription;
import org.moera.lib.node.types.notifications.AskedNotification;

public class AskedNotificationUtil {
    
    public static AskedNotification build(AskDescription askDescription) {
        AskedNotification notification = new AskedNotification();
        notification.setSubject(askDescription.getSubject());
        notification.setFriendGroupId(askDescription.getFriendGroupId());
        notification.setMessage(askDescription.getMessage());
        return notification;
    }

}
