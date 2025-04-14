package org.moera.node.model.notification;

import org.moera.lib.node.types.BlockedOperation;
import org.moera.lib.node.types.SearchBlockUpdate;
import org.moera.lib.node.types.SearchContentUpdateType;
import org.moera.lib.node.types.SearchFriendUpdate;
import org.moera.lib.node.types.SearchSubscriptionUpdate;
import org.moera.lib.node.types.notifications.SearchContentUpdatedNotification;

public class SearchContentUpdatedNotificationUtil {

    public static SearchContentUpdatedNotification buildProfileUpdate() {
        SearchContentUpdatedNotification notification = new SearchContentUpdatedNotification();
        notification.setUpdateType(SearchContentUpdateType.PROFILE);
        return notification;
    }

    public static SearchContentUpdatedNotification buildFriendUpdate(
        SearchContentUpdateType updateType, String nodeName
    ) {
        SearchContentUpdatedNotification notification = new SearchContentUpdatedNotification();
        notification.setUpdateType(updateType);
        SearchFriendUpdate details = new SearchFriendUpdate();
        details.setNodeName(nodeName);
        notification.setFriendUpdate(details);
        return notification;
    }

    public static SearchContentUpdatedNotification buildSubscriptionUpdate(
        SearchContentUpdateType updateType, String nodeName, String feedName
    ) {
        SearchContentUpdatedNotification notification = new SearchContentUpdatedNotification();
        notification.setUpdateType(updateType);
        SearchSubscriptionUpdate details = new SearchSubscriptionUpdate();
        details.setNodeName(nodeName);
        details.setFeedName(feedName);
        notification.setSubscriptionUpdate(details);
        return notification;
    }

    public static SearchContentUpdatedNotification buildBlockUpdate(
        SearchContentUpdateType updateType, String nodeName, BlockedOperation blockedOperation
    ) {
        SearchContentUpdatedNotification notification = new SearchContentUpdatedNotification();
        notification.setUpdateType(updateType);
        SearchBlockUpdate details = new SearchBlockUpdate();
        details.setNodeName(nodeName);
        details.setBlockedOperation(blockedOperation);
        notification.setBlockUpdate(details);
        return notification;
    }

}
