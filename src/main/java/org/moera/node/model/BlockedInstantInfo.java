package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.data.BlockedInstant;
import org.moera.node.data.StoryType;
import org.moera.node.util.Util;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BlockedInstantInfo {

    private String id;
    private StoryType storyType;
    private String entryId;
    private long createdAt;

    public BlockedInstantInfo() {
    }

    public BlockedInstantInfo(BlockedInstant blockedInstant) {
        id = blockedInstant.getId().toString();
        storyType = blockedInstant.getStoryType();
        if (blockedInstant.getEntry() != null) {
            entryId = blockedInstant.getEntry().getId().toString();
        }
        createdAt = Util.toEpochSecond(blockedInstant.getCreatedAt());
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

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

}
