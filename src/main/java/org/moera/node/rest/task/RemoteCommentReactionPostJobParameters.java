package org.moera.node.rest.task;

import org.moera.node.model.ReactionAttributes;

public class RemoteCommentReactionPostJobParameters {

    String targetNodeName;
    String postingId;
    String commentId;
    ReactionAttributes attributes;

    public RemoteCommentReactionPostJobParameters(String targetNodeName, String postingId, String commentId,
                                                  ReactionAttributes attributes) {
        this.targetNodeName = targetNodeName;
        this.postingId = postingId;
        this.commentId = commentId;
        this.attributes = attributes;
    }

    public String getTargetNodeName() {
        return targetNodeName;
    }

    public void setTargetNodeName(String targetNodeName) {
        this.targetNodeName = targetNodeName;
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

    public ReactionAttributes getAttributes() {
        return attributes;
    }

    public void setAttributes(ReactionAttributes attributes) {
        this.attributes = attributes;
    }

}
