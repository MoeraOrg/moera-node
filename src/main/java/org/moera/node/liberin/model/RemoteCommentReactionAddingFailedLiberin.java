package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.lib.node.types.CommentInfo;
import org.moera.lib.node.types.PostingInfo;
import org.moera.node.liberin.Liberin;

public class RemoteCommentReactionAddingFailedLiberin extends Liberin {

    private String nodeName;
    private String postingId;
    private PostingInfo postingInfo;
    private String commentId;
    private CommentInfo commentInfo;

    public RemoteCommentReactionAddingFailedLiberin(
        String nodeName, String postingId, PostingInfo postingInfo, String commentId, CommentInfo commentInfo
    ) {
        this.nodeName = nodeName;
        this.postingId = postingId;
        this.postingInfo = postingInfo;
        this.commentId = commentId;
        this.commentInfo = commentInfo;
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

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public CommentInfo getCommentInfo() {
        return commentInfo;
    }

    public void setCommentInfo(CommentInfo commentInfo) {
        this.commentInfo = commentInfo;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("nodeName", nodeName);
        model.put("postingId", postingId);
        model.put("posting", postingInfo);
        model.put("commentId", commentId);
        model.put("comment", commentInfo);
    }

}
