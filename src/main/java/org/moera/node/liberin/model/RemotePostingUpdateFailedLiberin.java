package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.lib.node.types.PostingInfo;
import org.moera.lib.node.types.WhoAmI;
import org.moera.node.liberin.Liberin;

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

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("nodeInfo", nodeInfo);
        model.put("postingId", postingId);
        model.put("prevPosting", prevPostingInfo);
    }

}
