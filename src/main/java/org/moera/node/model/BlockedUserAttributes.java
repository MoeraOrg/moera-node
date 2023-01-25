package org.moera.node.model;

import java.util.UUID;

import org.moera.node.data.BlockedOperation;
import org.moera.node.data.BlockedUser;
import org.moera.node.util.Util;

public class BlockedUserAttributes {

    private BlockedOperation blockedOperation;

    private String nodeName;

    private UUID entryId;

    private String entryNodeName;

    private String entryPostingId;

    private Long deadline;

    public BlockedOperation getBlockedOperation() {
        return blockedOperation;
    }

    public void setBlockedOperation(BlockedOperation blockedOperation) {
        this.blockedOperation = blockedOperation;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public UUID getEntryId() {
        return entryId;
    }

    public void setEntryId(UUID entryId) {
        this.entryId = entryId;
    }

    public String getEntryNodeName() {
        return entryNodeName;
    }

    public void setEntryNodeName(String entryNodeName) {
        this.entryNodeName = entryNodeName;
    }

    public String getEntryPostingId() {
        return entryPostingId;
    }

    public void setEntryPostingId(String entryPostingId) {
        this.entryPostingId = entryPostingId;
    }

    public Long getDeadline() {
        return deadline;
    }

    public void setDeadline(Long deadline) {
        this.deadline = deadline;
    }

    public void toBlockedInstant(BlockedUser blockedUser) {
        blockedUser.setBlockedOperation(blockedOperation);
        blockedUser.setRemoteNodeName(nodeName);
        blockedUser.setEntryNodeName(entryNodeName);
        blockedUser.setEntryPostingId(entryPostingId);
        blockedUser.setDeadline(Util.toTimestamp(deadline));
    }

}
