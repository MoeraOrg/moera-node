package org.moera.node.model.notification;

import org.moera.lib.node.types.notifications.SheriffOrderForCommentAddedNotification;

public class SheriffOrderForCommentAddedNotificationUtil {

    public static SheriffOrderForCommentAddedNotification build(
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
        SheriffOrderForCommentAddedNotification notification = new SheriffOrderForCommentAddedNotification();

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
