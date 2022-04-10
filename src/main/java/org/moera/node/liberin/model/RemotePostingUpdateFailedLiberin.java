package org.moera.node.liberin.model;

import org.moera.node.liberin.Liberin;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.WhoAmI;

public class RemotePostingUpdateFailedLiberin extends Liberin {

    private WhoAmI nodeInfo;
    private String postingId;
    private PostingInfo prevPostingInfo;

    public RemotePostingUpdateFailedLiberin(WhoAmI nodeInfo, String postingId, PostingInfo prevPostingInfo) {
        this.nodeInfo = nodeInfo;
        this.postingId = postingId;
        this.prevPostingInfo = prevPostingInfo;
    }

    public WhoAmI getNodeInfo() {
        return nodeInfo;
    }

    public void setNodeInfo(WhoAmI nodeInfo) {
        this.nodeInfo = nodeInfo;
    }

    public String getPostingId() {
        return postingId;
    }

    public void setPostingId(String postingId) {
        this.postingId = postingId;
    }

    public PostingInfo getPrevPostingInfo() {
        return prevPostingInfo;
    }

    public void setPrevPostingInfo(PostingInfo prevPostingInfo) {
        this.prevPostingInfo = prevPostingInfo;
    }

}
