package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.lib.node.types.CommentInfo;
import org.moera.lib.node.types.PostingInfo;
import org.moera.node.liberin.Liberin;

public class RemoteCommentUpdateFailedLiberin extends Liberin {

    private String remoteNodeName;
    private String remotePostingId;
    private PostingInfo postingInfo;
    private String remoteCommentId;
    private CommentInfo prevCommentInfo;

    public RemoteCommentUpdateFailedLiberin(
        String remoteNodeName,
        String remotePostingId,
        PostingInfo postingInfo,
        String remoteCommentId,
        CommentInfo prevCommentInfo
    ) {
        this.remoteNodeName = remoteNodeName;
        this.remotePostingId = remotePostingId;
        this.postingInfo = postingInfo;
        this.remoteCommentId = remoteCommentId;
        this.prevCommentInfo = prevCommentInfo;
    }

    public String getRemoteNodeName() {
        return remoteNodeName;
    }

    public void setRemoteNodeName(String remoteNodeName) {
        this.remoteNodeName = remoteNodeName;
    }

    public String getRemotePostingId() {
        return remotePostingId;
    }

    public void setRemotePostingId(String remotePostingId) {
        this.remotePostingId = remotePostingId;
    }

    public PostingInfo getPostingInfo() {
        return postingInfo;
    }

    public void setPostingInfo(PostingInfo postingInfo) {
        this.postingInfo = postingInfo;
    }

    public String getRemoteCommentId() {
        return remoteCommentId;
    }

    public void setRemoteCommentId(String remoteCommentId) {
        this.remoteCommentId = remoteCommentId;
    }

    public CommentInfo getPrevCommentInfo() {
        return prevCommentInfo;
    }

    public void setPrevCommentInfo(CommentInfo prevCommentInfo) {
        this.prevCommentInfo = prevCommentInfo;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("remoteNodeName", remoteNodeName);
        model.put("postingId", remotePostingId);
        model.put("posting", postingInfo);
        model.put("commentId", remoteCommentId);
        model.put("prevComment", prevCommentInfo);
    }

}
