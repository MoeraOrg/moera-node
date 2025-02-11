package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.lib.node.types.SubscriptionReason;
import org.moera.node.liberin.Liberin;

public class ForeignCommentDeletedLiberin extends Liberin {

    private String nodeName;
    private String postingId;
    private String ownerName;
    private String commentId;
    private SubscriptionReason reason;

    public ForeignCommentDeletedLiberin(String nodeName, String postingId, String ownerName, String commentId,
                                        SubscriptionReason reason) {
        this.nodeName = nodeName;
        this.postingId = postingId;
        this.ownerName = ownerName;
        this.commentId = commentId;
        this.reason = reason;
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

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public SubscriptionReason getReason() {
        return reason;
    }

    public void setReason(SubscriptionReason reason) {
        this.reason = reason;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("nodeName", nodeName);
        model.put("postingId", postingId);
        model.put("ownerName", ownerName);
        model.put("commentId", commentId);
        model.put("reason", reason);
    }

}
