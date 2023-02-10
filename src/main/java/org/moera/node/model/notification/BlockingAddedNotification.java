package org.moera.node.model.notification;

import javax.validation.constraints.Size;

import org.moera.node.data.BlockedOperation;

public class BlockingAddedNotification extends Notification {

    private BlockedOperation blockedOperation;

    @Size(max = 40)
    private String postingId;

    @Size(max = 255)
    private String postingHeading;

    private Long deadline;

    @Size(max = 1024)
    private String reason;

    public BlockingAddedNotification() {
        super(NotificationType.BLOCKING_ADDED);
    }

    public BlockingAddedNotification(BlockedOperation blockedOperation, String postingId, String postingHeading,
                                     Long deadline, String reason) {
        super(NotificationType.BLOCKING_ADDED);
        this.blockedOperation = blockedOperation;
        this.postingId = postingId;
        this.postingHeading = postingHeading;
        this.deadline = deadline;
        this.reason = reason;
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

    public Long getDeadline() {
        return deadline;
    }

    public void setDeadline(Long deadline) {
        this.deadline = deadline;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

}
