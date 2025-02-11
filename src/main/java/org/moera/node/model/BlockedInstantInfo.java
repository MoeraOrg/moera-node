package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.lib.node.types.StoryType;
import org.moera.node.data.BlockedInstant;
import org.moera.node.util.Util;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BlockedInstantInfo {

    private String id;
    private StoryType storyType;
    private String entryId;
    private String remoteNodeName;
    private String remotePostingId;
    private String remoteOwnerName;
    private long createdAt;
    private Long deadline;

    public BlockedInstantInfo() {
    }

    public BlockedInstantInfo(BlockedInstant blockedInstant) {
        id = blockedInstant.getId().toString();
        storyType = blockedInstant.getStoryType();
        if (blockedInstant.getEntry() != null) {
            entryId = blockedInstant.getEntry().getId().toString();
        }
        remoteNodeName = blockedInstant.getRemoteNodeName();
        remotePostingId = blockedInstant.getRemotePostingId();
        remoteOwnerName = blockedInstant.getRemoteOwnerName();
        createdAt = Util.toEpochSecond(blockedInstant.getCreatedAt());
        deadline = Util.toEpochSecond(blockedInstant.getDeadline());
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public StoryType getStoryType() {
        return storyType;
    }

    public void setStoryType(StoryType storyType) {
        this.storyType = storyType;
    }

    public String getEntryId() {
        return entryId;
    }

    public void setEntryId(String entryId) {
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

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getDeadline() {
        return deadline;
    }

    public void setDeadline(Long deadline) {
        this.deadline = deadline;
    }

}
