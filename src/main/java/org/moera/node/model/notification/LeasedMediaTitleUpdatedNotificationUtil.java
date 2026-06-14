package org.moera.node.model.notification;

import java.util.UUID;

import org.moera.lib.node.types.notifications.LeasedMediaTitleUpdatedNotification;

public class LeasedMediaTitleUpdatedNotificationUtil {

    public static LeasedMediaTitleUpdatedNotification build(UUID mediaId, String title) {
        LeasedMediaTitleUpdatedNotification notification = new LeasedMediaTitleUpdatedNotification();
        notification.setMediaId(mediaId.toString());
        notification.setTitle(title);
        return notification;
    }

}
