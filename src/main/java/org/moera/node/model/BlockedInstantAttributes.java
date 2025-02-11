package org.moera.node.model;

import java.util.UUID;

import org.moera.lib.node.types.StoryType;
import org.moera.node.data.BlockedInstant;
import org.moera.node.util.Util;

public class BlockedInstantAttributes {

    private StoryType storyType;

    private UUID entryId;

    private String remoteNodeName;

    private String remotePostingId;

    private String remoteOwnerName;

    private Long deadline;

    public StoryType getStoryType() {
        return storyType;
    }

    public void setStoryType(StoryType storyType) {
        this.storyType = storyType;
    }

    public UUID getEntryId() {
        return entryId;
    }

    public void setEntryId(UUID entryId) {
        this.entryId = entryId;
    }

    public String getRemoteNodeName() {
        return remoteNodeName;
    }

    public void setRemoteNodeName(String remoteNodeName) {
        this.remoteNodeName = remoteNodeName;
    }

    public String getRemotePostingId() {
        return remotePostingId;
    }

    public void setRemotePostingId(String remotePostingId) {
        this.remotePostingId = remotePostingId;
    }

    public String getRemoteOwnerName() {
        return remoteOwnerName;
    }

    public void setRemoteOwnerName(String remoteOwnerName) {
        this.remoteOwnerName = remoteOwnerName;
    }

    public Long getDeadline() {
        return deadline;
    }

    public void setDeadline(Long deadline) {
        this.deadline = deadline;
    }

    public void toBlockedInstant(BlockedInstant blockedInstant) {
        blockedInstant.setStoryType(storyType);
        blockedInstant.setRemoteNodeName(remoteNodeName);
        blockedInstant.setRemotePostingId(remotePostingId);
        blockedInstant.setRemoteOwnerName(remoteOwnerName);
        blockedInstant.setDeadline(Util.toTimestamp(deadline));
    }

}
