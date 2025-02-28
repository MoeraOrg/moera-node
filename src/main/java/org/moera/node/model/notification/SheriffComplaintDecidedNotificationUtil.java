package org.moera.node.model.notification;

import org.moera.lib.node.types.notifications.SheriffComplaintDecidedNotification;

public class SheriffComplaintDecidedNotificationUtil {
    
    public static SheriffComplaintDecidedNotification build(
        String remoteNodeName,
        String remoteFeedName,
        String postingOwnerName,
        String postingOwnerFullName,
        String postingHeading,
        String postingId,
        String commentOwnerName,
        String commentOwnerFullName,
        String commentHeading,
        String commentId,
        String complaintGroupId
    ) {
        SheriffComplaintDecidedNotification notification = new SheriffComplaintDecidedNotification();

        notification.setRemoteNodeName(remoteNodeName);
        notification.setRemoteFeedName(remoteFeedName);
        notification.setPostingOwnerName(postingOwnerName);
        notification.setPostingOwnerFullName(postingOwnerFullName);
        notification.setPostingHeading(postingHeading);
        notification.setPostingId(postingId);
        notification.setCommentOwnerName(commentOwnerName);
        notification.setCommentOwnerFullName(commentOwnerFullName);
        notification.setCommentHeading(commentHeading);
        notification.setCommentId(commentId);
        notification.setComplaintGroupId(complaintGroupId);
        
        return notification;
    }

}
