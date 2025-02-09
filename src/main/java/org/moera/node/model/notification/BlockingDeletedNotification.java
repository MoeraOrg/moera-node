package org.moera.node.model.notification;

import jakarta.validation.constraints.Size;

import org.moera.node.data.BlockedOperation;

public class BlockingDeletedNotification extends Notification {

    private BlockedOperation blockedOperation;

    @Size(max = 40)
    private String postingId;

    @Size(max = 255)
    private String postingHeading;

    public BlockingDeletedNotification() {
        super(NotificationType.BLOCKING_DELETED);
    }

    public BlockingDeletedNotification(BlockedOperation blockedOperation, String postingId, String postingHeading) {
        super(NotificationType.BLOCKING_DELETED);
        this.blockedOperation = blockedOperation;
        this.postingId = postingId;
        this.postingHeading = postingHeading;
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

    public String getPostingHeading() {
        return postingHeading;
    }

    public void setPostingHeading(String postingHeading) {
        this.postingHeading = postingHeading;
    }

}
