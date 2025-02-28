package org.moera.node.model.notification;

import org.moera.lib.node.types.BlockedOperation;
import org.moera.lib.node.types.notifications.BlockingDeletedNotification;

public class BlockingDeletedNotificationUtil {
    
    public static BlockingDeletedNotification build(
        BlockedOperation blockedOperation, String postingId, String postingHeading
    ) {
        BlockingDeletedNotification notification = new BlockingDeletedNotification();
        notification.setBlockedOperation(blockedOperation);
        notification.setPostingId(postingId);
        notification.setPostingHeading(postingHeading);
        return notification;
    }

}
