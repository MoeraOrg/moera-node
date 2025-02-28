package org.moera.node.model.notification;

import org.moera.lib.node.types.BlockedOperation;
import org.moera.lib.node.types.notifications.BlockingAddedNotification;

public class BlockingAddedNotificationUtil {
    
    public static BlockingAddedNotification build(
        BlockedOperation blockedOperation,
        String postingId,
        String postingHeading,
        Long deadline,
        String reason
    ) {
        BlockingAddedNotification notification = new BlockingAddedNotification();
        notification.setBlockedOperation(blockedOperation);
        notification.setPostingId(postingId);
        notification.setPostingHeading(postingHeading);
        notification.setDeadline(deadline);
        notification.setReason(reason);
        return notification;
    }

}
