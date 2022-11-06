package org.moera.node.model;

import java.util.Objects;

public class RemoteFeed {

    private String nodeName;
    private String feedName;

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getFeedName() {
        return feedName;
    }

    public void setFeedName(String feedName) {
        this.feedName = feedName;
    }

    @Override
    public boolean equals(Object peer) {
        if (this == peer) {
            return true;
        }
        if (peer == null || getClass() != peer.getClass()) {
            return false;
        }
        RemoteFeed that = (RemoteFeed) peer;
        return Objects.equals(nodeName, that.nodeName) && Objects.equals(feedName, that.feedName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeName, feedName);
    }

}
