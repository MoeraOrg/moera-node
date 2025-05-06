package org.moera.node.model.notification;

import org.moera.lib.node.types.notifications.SheriffOrderForFeedAddedNotification;

public class SheriffOrderForFeedAddedNotificationUtil {
    
    public static SheriffOrderForFeedAddedNotification build(
        String remoteNodeName,
        String remoteFeedName,
        String orderId
    ) {
        SheriffOrderForFeedAddedNotification notification = new SheriffOrderForFeedAddedNotification();
        notification.setRemoteNodeName(remoteNodeName);
        notification.setRemoteFeedName(remoteFeedName);
        notification.setOrderId(orderId);
        return notification;
    }

}
