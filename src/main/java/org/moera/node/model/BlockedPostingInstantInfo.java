package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.data.BlockedInstant;
import org.moera.node.data.StoryType;
import org.moera.node.util.Util;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BlockedPostingInstantInfo {

    private String id;
    private StoryType storyType;
    private String remoteOwnerName;
    private Long deadline;

    public BlockedPostingInstantInfo() {
    }

    public BlockedPostingInstantInfo(BlockedInstant blockedInstant) {
        id = blockedInstant.getId().toString();
        storyType = blockedInstant.getStoryType();
        remoteOwnerName = blockedInstant.getRemoteOwnerName();
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

}
