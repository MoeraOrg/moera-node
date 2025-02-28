package org.moera.node.model.notification;

import org.moera.lib.node.types.notifications.SheriffOrderForPostingAddedNotification;

public class SheriffOrderForPostingAddedNotificationUtil {
    
    public static SheriffOrderForPostingAddedNotification build(
        String remoteNodeName,
        String remoteFeedName,
        String postingHeading,
        String postingId,
        String orderId
    ) {
        SheriffOrderForPostingAddedNotification notification = new SheriffOrderForPostingAddedNotification();
        notification.setRemoteNodeName(remoteNodeName);
        notification.setRemoteFeedName(remoteFeedName);
        notification.setPostingHeading(postingHeading);
        notification.setPostingId(postingId);
        notification.setOrderId(orderId);
        return notification;
    }

}
