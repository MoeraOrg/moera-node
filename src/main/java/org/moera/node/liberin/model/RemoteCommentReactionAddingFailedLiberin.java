package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.node.liberin.Liberin;
import org.moera.node.model.CommentInfo;
import org.moera.node.model.PostingInfo;

public class RemoteCommentReactionAddingFailedLiberin extends Liberin {

    private String postingId;
    private PostingInfo postingInfo;
    private String commentId;
    private CommentInfo commentInfo;

    public RemoteCommentReactionAddingFailedLiberin(String postingId, PostingInfo postingInfo, String commentId,
                                                    CommentInfo commentInfo) {
        this.postingId = postingId;
        this.postingInfo = postingInfo;
        this.commentId = commentId;
        this.commentInfo = commentInfo;
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
        model.put("postingId", postingId);
        model.put("posting", postingInfo);
        model.put("commentId", commentId);
        model.put("comment", commentInfo);
    }

}
