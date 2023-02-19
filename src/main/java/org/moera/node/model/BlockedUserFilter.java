package org.moera.node.model;

import java.util.UUID;

import org.moera.node.data.BlockedOperation;

public class BlockedUserFilter {

    private BlockedOperation[] blockedOperations;
    private String nodeName;
    private UUID entryId;
    private String entryNodeName;
    private String entryPostingId;
    private Boolean strict;

    public BlockedOperation[] getBlockedOperations() {
        return blockedOperations;
    }

    public void setBlockedOperations(BlockedOperation[] blockedOperations) {
        this.blockedOperations = blockedOperations;
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

    public Boolean getStrict() {
        return strict;
    }

    public void setStrict(Boolean strict) {
        this.strict = strict;
    }

}
