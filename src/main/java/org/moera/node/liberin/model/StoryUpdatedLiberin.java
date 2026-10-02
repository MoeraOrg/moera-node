package org.moera.node.liberin.model;

import java.util.UUID;

import org.moera.node.liberin.Liberin;

public class StoryUpdatedLiberin extends Liberin {

    private UUID storyId;

    public StoryUpdatedLiberin(UUID storyId) {
        this.storyId = storyId;
    }

    public UUID getStoryId() {
        return storyId;
    }

    public void setStoryId(UUID storyId) {
        this.storyId = storyId;
    }

}
