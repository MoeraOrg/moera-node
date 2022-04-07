package org.moera.node.liberin.model;

import org.moera.node.liberin.Liberin;
import org.moera.node.model.CommentInfo;
import org.moera.node.model.PostingInfo;

public class RemoteCommentMediaReactionAddingFailedLiberin extends Liberin {

    private String postingId;
    private String parentMediaId;
    private PostingInfo parentPostingInfo;
    private CommentInfo parentCommentInfo;

    public RemoteCommentMediaReactionAddingFailedLiberin(String postingId, String parentMediaId,
                                                         PostingInfo parentPostingInfo, CommentInfo parentCommentInfo) {
        this.postingId = postingId;
        this.parentMediaId = parentMediaId;
        this.parentPostingInfo = parentPostingInfo;
        this.parentCommentInfo = parentCommentInfo;
    }

    public String getPostingId() {
        return postingId;
    }

    public void setPostingId(String postingId) {
        this.postingId = postingId;
    }

    public String getParentMediaId() {
        return parentMediaId;
    }

    public void setParentMediaId(String parentMediaId) {
        this.parentMediaId = parentMediaId;
    }

    public PostingInfo getParentPostingInfo() {
        return parentPostingInfo;
    }

    public void setParentPostingInfo(PostingInfo parentPostingInfo) {
        this.parentPostingInfo = parentPostingInfo;
    }

    public CommentInfo getParentCommentInfo() {
        return parentCommentInfo;
    }

    public void setParentCommentInfo(CommentInfo parentCommentInfo) {
        this.parentCommentInfo = parentCommentInfo;
    }

}
