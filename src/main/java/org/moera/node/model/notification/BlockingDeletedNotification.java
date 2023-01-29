package org.moera.node.model.notification;

import org.moera.node.data.BlockedOperation;

public class BlockingDeletedNotification extends Notification {

    private BlockedOperation blockedOperation;
    private String postingId;

    public BlockingDeletedNotification() {
        super(NotificationType.BLOCKING_DELETED);
    }

    public BlockingDeletedNotification(BlockedOperation blockedOperation, String postingId) {
        super(NotificationType.BLOCKING_DELETED);
        this.blockedOperation = blockedOperation;
        this.postingId = postingId;
    }

    public BlockedOperation getBlockedOperation() {
        return blockedOperation;
    }

    public void setBlockedOperation(BlockedOperation blockedOperation) {
        this.blockedOperation = blockedOperation;
    }

    public String getPostingId() {
        return postingId;
    }

    public void setPostingId(String postingId) {
        this.postingId = postingId;
    }

}
