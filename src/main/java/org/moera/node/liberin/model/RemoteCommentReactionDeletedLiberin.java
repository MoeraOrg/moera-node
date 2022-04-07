package org.moera.node.liberin.model;

import org.moera.node.liberin.Liberin;

public class RemoteCommentReactionDeletedLiberin extends Liberin {

    private String nodeName;
    private String postingId;
    private String commentId;
    private String ownerName;
    private boolean negative;

    public RemoteCommentReactionDeletedLiberin(String nodeName, String postingId, String commentId, String ownerName,
                                               boolean negative) {
        this.nodeName = nodeName;
        this.postingId = postingId;
        this.commentId = commentId;
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

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
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

}
