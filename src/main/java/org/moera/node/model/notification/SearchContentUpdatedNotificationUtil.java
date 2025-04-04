package org.moera.node.model.notification;

import org.moera.lib.node.types.SearchContentUpdateType;
import org.moera.lib.node.types.notifications.SearchContentUpdatedNotification;

public class SearchContentUpdatedNotificationUtil {

    public static SearchContentUpdatedNotification build(SearchContentUpdateType updateType) {
        SearchContentUpdatedNotification notification = new SearchContentUpdatedNotification();
        notification.setUpdateType(updateType);
        return notification;
    }

}
