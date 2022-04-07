package org.moera.node.liberin.model;

import org.moera.node.liberin.Liberin;
import org.moera.node.model.CommentInfo;
import org.moera.node.model.PostingInfo;

public class RemoteCommentUpdateFailedLiberin extends Liberin {

    private String postingId;
    private PostingInfo postingInfo;
    private String commentId;
    private CommentInfo prevCommentInfo;

    public RemoteCommentUpdateFailedLiberin(String postingId, PostingInfo postingInfo, String commentId,
                                            CommentInfo prevCommentInfo) {
        this.postingId = postingId;
        this.postingInfo = postingInfo;
        this.commentId = commentId;
        this.prevCommentInfo = prevCommentInfo;
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

    public CommentInfo getPrevCommentInfo() {
        return prevCommentInfo;
    }

    public void setPrevCommentInfo(CommentInfo prevCommentInfo) {
        this.prevCommentInfo = prevCommentInfo;
    }

}
