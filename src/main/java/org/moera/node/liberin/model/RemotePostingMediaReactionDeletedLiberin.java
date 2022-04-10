package org.moera.node.liberin.model;

import org.moera.node.liberin.Liberin;

public class RemotePostingMediaReactionDeletedLiberin extends Liberin {

    private String nodeName;
    private String postingId;
    private String ownerName;
    private boolean negative;

    public RemotePostingMediaReactionDeletedLiberin(String nodeName, String postingId, String ownerName,
                                                    boolean negative) {
        this.nodeName = nodeName;
        this.postingId = postingId;
        this.ownerName = ownerName;
        this.negative = negative;
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

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public boolean isNegative() {
        return negative;
    }

    public void setNegative(boolean negative) {
        this.negative = negative;
    }

}