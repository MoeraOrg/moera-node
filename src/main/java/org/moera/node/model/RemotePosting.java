package org.moera.node.model;

import java.util.Objects;

public class RemotePosting {

    private String nodeName;
    private String postingId;

    public RemotePosting() {
    }

    public RemotePosting(String nodeName, String postingId) {
        this.nodeName = nodeName;
        this.postingId = postingId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getPostingId() {
        return postingId;
    }

    public void setPostingId(String postingId) {
        this.postingId = postingId;
    }

    @Override
    public boolean equals(Object peer) {
        if (this == peer) {
            return true;
        }
        if (peer == null || getClass() != peer.getClass()) {
            return false;
        }
        RemotePosting that = (RemotePosting) peer;
        return Objects.equals(nodeName, that.nodeName) && Objects.equals(postingId, that.postingId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeName, postingId);
    }

}
