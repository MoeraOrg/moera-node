package org.moera.node.liberin.model;

import org.moera.node.liberin.Liberin;

public class MentionInRemoteCommentDeletedLiberin extends Liberin {

    private String nodeName;
    private String postingId;
    private String commentId;

    public MentionInRemoteCommentDeletedLiberin(String nodeName, String postingId, String commentId) {
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

}
