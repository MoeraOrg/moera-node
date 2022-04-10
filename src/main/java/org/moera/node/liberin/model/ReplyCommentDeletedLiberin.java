package org.moera.node.liberin.model;

import org.moera.node.liberin.Liberin;

public class ReplyCommentDeletedLiberin extends Liberin {

    private String nodeName;
    private String postingId;
    private String commentId;
    private String commentOwnerName;

    public ReplyCommentDeletedLiberin(String nodeName, String postingId, String commentId, String commentOwnerName) {
        this.nodeName = nodeName;
        this.postingId = postingId;
        this.commentId = commentId;
        this.commentOwnerName = commentOwnerName;
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

    public String getCommentOwnerName() {
        return commentOwnerName;
    }

    public void setCommentOwnerName(String commentOwnerName) {
        this.commentOwnerName = commentOwnerName;
    }

}
