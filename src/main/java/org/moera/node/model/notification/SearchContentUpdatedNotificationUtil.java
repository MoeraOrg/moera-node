package org.moera.node.model.notification;

import org.moera.lib.node.types.BlockedOperation;
import org.moera.lib.node.types.SearchContentUpdateType;
import org.moera.lib.node.types.notifications.SearchContentUpdatedNotification;

public class SearchContentUpdatedNotificationUtil {

    public static SearchContentUpdatedNotification build(SearchContentUpdateType updateType) {
        return build(updateType, null, null, null);
    }

    public static SearchContentUpdatedNotification build(SearchContentUpdateType updateType, String nodeName) {
        return build(updateType, nodeName, null, null);
    }

    public static SearchContentUpdatedNotification build(
        SearchContentUpdateType updateType, String nodeName, String feedName
    ) {
        return build(updateType, nodeName, feedName, null);
    }

    public static SearchContentUpdatedNotification build(
        SearchContentUpdateType updateType, String nodeName, BlockedOperation blockedOperation
    ) {
        return build(updateType, nodeName, null, blockedOperation);
    }

    public static SearchContentUpdatedNotification build(
        SearchContentUpdateType updateType,
        String nodeName,
        String feedName,
        BlockedOperation blockedOperation
    ) {
        SearchContentUpdatedNotification notification = new SearchContentUpdatedNotification();
        notification.setUpdateType(updateType);
        notification.setNodeName(nodeName);
        notification.setFeedName(feedName);
        notification.setBlockedOperation(blockedOperation);
        return notification;
    }

}
