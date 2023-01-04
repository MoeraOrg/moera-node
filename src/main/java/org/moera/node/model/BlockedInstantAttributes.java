package org.moera.node.model;

import java.util.UUID;

import org.moera.node.data.StoryType;

public class BlockedInstantAttributes {

    private StoryType storyType;

    private UUID entryId;

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

}
