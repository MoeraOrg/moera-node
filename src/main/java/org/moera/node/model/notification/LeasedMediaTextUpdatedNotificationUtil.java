package org.moera.node.model.notification;

import java.util.UUID;

import org.moera.lib.node.types.notifications.LeasedMediaTextUpdatedNotification;

public class LeasedMediaTextUpdatedNotificationUtil {

    public static LeasedMediaTextUpdatedNotification build(UUID mediaId, String textContent) {
        LeasedMediaTextUpdatedNotification notification = new LeasedMediaTextUpdatedNotification();
        notification.setMediaId(mediaId.toString());
        notification.setTextContent(textContent);
        return notification;
    }

}
