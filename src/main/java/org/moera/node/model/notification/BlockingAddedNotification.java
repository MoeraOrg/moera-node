package org.moera.node.model.notification;

import org.moera.node.data.BlockedOperation;

public class BlockingAddedNotification extends Notification {

    private BlockedOperation blockedOperation;
    private String postingId;
    private Long deadline;

    public BlockingAddedNotification() {
        super(NotificationType.BLOCKING_ADDED);
    }

    public BlockingAddedNotification(BlockedOperation blockedOperation, String postingId, Long deadline) {
        super(NotificationType.BLOCKING_ADDED);
        this.blockedOperation = blockedOperation;
        this.postingId = postingId;
        this.deadline = deadline;
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

    public Long getDeadline() {
        return deadline;
    }

    public void setDeadline(Long deadline) {
        this.deadline = deadline;
    }

}
