package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.lib.node.types.PostingInfo;
import org.moera.node.liberin.Liberin;

public class RemotePostingReactionAddingFailedLiberin extends Liberin {

    private String nodeName;
    private String postingId;
    private PostingInfo postingInfo;

    public RemotePostingReactionAddingFailedLiberin(String nodeName, String postingId, PostingInfo postingInfo) {
        this.nodeName = nodeName;
        this.postingId = postingId;
        this.postingInfo = postingInfo;
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

    public PostingInfo getPostingInfo() {
        return postingInfo;
    }

    public void setPostingInfo(PostingInfo postingInfo) {
        this.postingInfo = postingInfo;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("nodeName", nodeName);
        model.put("postingId", postingId);
        model.put("posting", postingInfo);
    }

}
