package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.node.liberin.Liberin;
import org.moera.node.model.PostingInfo;

public class RemotePostingMediaReactionAddingFailedLiberin extends Liberin {

    private String nodeName;
    private String postingId;
    private String parentPostingId;
    private String parentMediaId;
    private PostingInfo parentPostingInfo;

    public RemotePostingMediaReactionAddingFailedLiberin(String nodeName, String postingId, String parentPostingId,
                                                         String parentMediaId, PostingInfo parentPostingInfo) {
        this.nodeName = nodeName;
        this.postingId = postingId;
        this.parentPostingId = parentPostingId;
        this.parentMediaId = parentMediaId;
        this.parentPostingInfo = parentPostingInfo;
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

    public String getParentPostingId() {
        return parentPostingId;
    }

    public void setParentPostingId(String parentPostingId) {
        this.parentPostingId = parentPostingId;
    }

    public String getParentMediaId() {
        return parentMediaId;
    }

    public void setParentMediaId(String parentMediaId) {
        this.parentMediaId = parentMediaId;
    }

    public PostingInfo getParentPostingInfo() {
        return parentPostingInfo;
    }

    public void setParentPostingInfo(PostingInfo parentPostingInfo) {
        this.parentPostingInfo = parentPostingInfo;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("nodeName", nodeName);
        model.put("postingId", postingId);
        model.put("parentPostingId", parentPostingId);
        model.put("parentMediaId", parentMediaId);
        model.put("parentPosting", parentPostingInfo);
    }

}
