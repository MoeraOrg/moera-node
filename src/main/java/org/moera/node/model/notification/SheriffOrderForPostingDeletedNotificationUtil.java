package org.moera.node.model.notification;

import org.moera.lib.node.types.notifications.SheriffOrderForPostingDeletedNotification;

public class SheriffOrderForPostingDeletedNotificationUtil {
    
    public static SheriffOrderForPostingDeletedNotification build(
        String remoteNodeName,
        String remoteFeedName,
        String postingHeading,
        String postingId,
        String orderId
    ) {
        SheriffOrderForPostingDeletedNotification notification = new SheriffOrderForPostingDeletedNotification();
        notification.setRemoteNodeName(remoteNodeName);
        notification.setRemoteFeedName(remoteFeedName);
        notification.setPostingHeading(postingHeading);
        notification.setPostingId(postingId);
        notification.setOrderId(orderId);
        return notification;
    }

}
