package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.lib.node.types.CommentInfo;
import org.moera.lib.node.types.PostingInfo;
import org.moera.node.liberin.Liberin;

public class RemoteCommentUpdatedLiberin extends Liberin {

    private String nodeName;
    private PostingInfo postingInfo;
    private CommentInfo commentInfo;
    private boolean videoCompressionWaited;

    public RemoteCommentUpdatedLiberin(String nodeName, String postingId, String commentId) {
        this.nodeName = nodeName;
        postingInfo = new PostingInfo();
        postingInfo.setId(postingId);
        commentInfo = new CommentInfo();
        commentInfo.setId(commentId);
    }

    public RemoteCommentUpdatedLiberin(
        String nodeName,
        PostingInfo postingInfo,
        CommentInfo commentInfo,
        boolean videoCompressionWaited
    ) {
        this.nodeName = nodeName;
        this.postingInfo = postingInfo;
        this.commentInfo = commentInfo;
        this.videoCompressionWaited = videoCompressionWaited;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public PostingInfo getPostingInfo() {
        return postingInfo;
    }

    public void setPostingInfo(PostingInfo postingInfo) {
        this.postingInfo = postingInfo;
    }

    public CommentInfo getCommentInfo() {
        return commentInfo;
    }

    public void setCommentInfo(CommentInfo commentInfo) {
        this.commentInfo = commentInfo;
    }

    public boolean isVideoCompressionWaited() {
        return videoCompressionWaited;
    }

    public void setVideoCompressionWaited(boolean videoCompressionWaited) {
        this.videoCompressionWaited = videoCompressionWaited;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("nodeName", nodeName);
        model.put("postingId", postingInfo.getId());
        model.put("commentId", commentInfo.getId());
    }

}
