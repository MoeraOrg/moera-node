package org.moera.node.liberin.model;

import org.moera.node.liberin.Liberin;

public class RemotePostingUpdatedLiberin extends Liberin {

    private String nodeName;
    private String postingId;

    public RemotePostingUpdatedLiberin(String nodeName, String postingId) {
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

}
