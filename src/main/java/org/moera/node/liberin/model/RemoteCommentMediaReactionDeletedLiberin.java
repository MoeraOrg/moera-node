package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.node.liberin.Liberin;

public class RemoteCommentMediaReactionDeletedLiberin extends Liberin {

    private String nodeName;
    private String postingId;
    private String ownerName;
    private boolean negative;

    public RemoteCommentMediaReactionDeletedLiberin(String nodeName, String postingId, String ownerName,
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

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("nodeName", nodeName);
        model.put("postingId", postingId);
        model.put("ownerName", ownerName);
        model.put("negative", negative);
    }

}
