package org.moera.node.model;

import org.moera.node.data.Story;

public class StoryOfRemoteNodeInfo extends StoryInfo {

    private String remoteNodeName;

    public StoryOfRemoteNodeInfo() {
    }

    protected StoryOfRemoteNodeInfo(Story story, boolean isAdmin) {
        super(story, isAdmin);

        remoteNodeName = story.getRemoteNodeName();
    }

    public String getRemoteNodeName() {
        return remoteNodeName;
    }

    public void setRemoteNodeName(String remoteNodeName) {
        this.remoteNodeName = remoteNodeName;
    }

}
