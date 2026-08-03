package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.lib.node.types.PostingInfo;
import org.moera.lib.node.types.WhoAmI;
import org.moera.node.liberin.Liberin;

public class RemotePostingAddedLiberin extends Liberin {

    private WhoAmI nodeInfo;
    private PostingInfo postingInfo;
    private boolean videoCompressionWaited;

    public RemotePostingAddedLiberin(
        WhoAmI nodeInfo,
        PostingInfo postingInfo,
        boolean videoCompressionWaited
    ) {
        this.nodeInfo = nodeInfo;
        this.postingInfo = postingInfo;
        this.videoCompressionWaited = videoCompressionWaited;
    }

    public WhoAmI getNodeInfo() {
        return nodeInfo;
    }

    public void setNodeInfo(WhoAmI nodeInfo) {
        this.nodeInfo = nodeInfo;
    }

    public PostingInfo getPostingInfo() {
        return postingInfo;
    }

    public void setPostingInfo(PostingInfo postingInfo) {
        this.postingInfo = postingInfo;
    }

    public boolean isVideoCompressionWaited() {
        return videoCompressionWaited;
    }

    public void setVideoCompressionWaited(boolean videoCompressionWaited) {
        this.videoCompressionWaited = videoCompressionWaited;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("nodeName", nodeInfo.getNodeName());
        model.put("postingId", postingInfo.getId());
    }

}
