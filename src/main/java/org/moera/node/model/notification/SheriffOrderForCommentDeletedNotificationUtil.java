package org.moera.node.model.notification;

import org.moera.lib.node.types.notifications.SheriffOrderForCommentDeletedNotification;

public class SheriffOrderForCommentDeletedNotificationUtil {
    
    public static SheriffOrderForCommentDeletedNotification build(
        String remoteNodeName,
        String remoteFeedName,
        String postingOwnerName,
        String postingOwnerFullName,
        String postingHeading,
        String postingId,
        String commentHeading,
        String commentId,
        String orderId
    ) {
        SheriffOrderForCommentDeletedNotification notification = new SheriffOrderForCommentDeletedNotification();
            
        notification.setRemoteNodeName(remoteNodeName);
        notification.setRemoteFeedName(remoteFeedName);
        notification.setPostingOwnerName(postingOwnerName);
        notification.setPostingOwnerFullName(postingOwnerFullName);
        notification.setPostingHeading(postingHeading);
        notification.setPostingId(postingId);
        notification.setCommentHeading(commentHeading);
        notification.setCommentId(commentId);
        notification.setOrderId(orderId);
        
        return notification;
    }

}
