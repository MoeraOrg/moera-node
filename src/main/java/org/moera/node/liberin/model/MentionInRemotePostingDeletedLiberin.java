package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.node.liberin.Liberin;

public class MentionInRemotePostingDeletedLiberin extends Liberin {

    private String nodeName;
    private String postingId;

    public MentionInRemotePostingDeletedLiberin(String nodeName, String postingId) {
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
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("nodeName", nodeName);
        model.put("postingId", postingId);
    }

}
