package org.moera.node.model.notification;

import org.moera.lib.node.types.notifications.SheriffOrderForFeedDeletedNotification;

public class SheriffOrderForFeedDeletedNotificationUtil {
    
    public static SheriffOrderForFeedDeletedNotification build(
        String remoteNodeName,
        String remoteFeedName,
        String orderId
    ) {
        SheriffOrderForFeedDeletedNotification notification = new SheriffOrderForFeedDeletedNotification();
        notification.setRemoteNodeName(remoteNodeName);
        notification.setRemoteFeedName(remoteFeedName);
        notification.setOrderId(orderId);
        return notification;
    }

}
