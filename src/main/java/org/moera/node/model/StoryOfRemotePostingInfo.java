package org.moera.node.model;

import org.moera.node.data.Story;

public class StoryOfRemotePostingInfo extends StoryInfo {

    private String remoteNodeName;
    private String remotePostingId;

    public StoryOfRemotePostingInfo() {
    }

    protected StoryOfRemotePostingInfo(Story story, boolean isAdmin) {
        super(story, isAdmin);

        remoteNodeName = story.getRemoteNodeName();
        remotePostingId = story.getRemoteEntryId();
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

}
