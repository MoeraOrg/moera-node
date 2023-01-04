package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.data.BlockedInstant;
import org.moera.node.data.StoryType;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BlockedPostingInstantInfo {

    private String id;
    private StoryType storyType;

    public BlockedPostingInstantInfo() {
    }

    public BlockedPostingInstantInfo(BlockedInstant blockedInstant) {
        id = blockedInstant.getId().toString();
        storyType = blockedInstant.getStoryType();
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

}
