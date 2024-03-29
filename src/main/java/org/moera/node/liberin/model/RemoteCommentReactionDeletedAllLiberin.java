package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.node.liberin.Liberin;

public class RemoteCommentReactionDeletedAllLiberin extends Liberin {

    private String nodeName;
    private String postingId;
    private String commentId;

    public RemoteCommentReactionDeletedAllLiberin(String nodeName, String postingId, String commentId) {
        this.nodeName = nodeName;
        this.postingId = postingId;
        this.commentId = commentId;
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

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("nodeName", nodeName);
        model.put("postingId", postingId);
        model.put("commentId", commentId);
    }

}
