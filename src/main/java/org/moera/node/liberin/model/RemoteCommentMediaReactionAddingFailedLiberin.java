package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.node.liberin.Liberin;
import org.moera.node.model.CommentInfo;
import org.moera.node.model.PostingInfo;

public class RemoteCommentMediaReactionAddingFailedLiberin extends Liberin {

    private String nodeName;
    private String mediaPostingId;
    private String parentMediaId;
    private PostingInfo parentPostingInfo;
    private CommentInfo parentCommentInfo;

    public RemoteCommentMediaReactionAddingFailedLiberin(String nodeName, String mediaPostingId, String parentMediaId,
                                                         PostingInfo parentPostingInfo, CommentInfo parentCommentInfo) {
        this.nodeName = nodeName;
        this.mediaPostingId = mediaPostingId;
        this.parentMediaId = parentMediaId;
        this.parentPostingInfo = parentPostingInfo;
        this.parentCommentInfo = parentCommentInfo;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getMediaPostingId() {
        return mediaPostingId;
    }

    public void setMediaPostingId(String mediaPostingId) {
        this.mediaPostingId = mediaPostingId;
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

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("nodeName", nodeName);
        model.put("mediaPostingId", mediaPostingId);
        model.put("parentMediaId", parentMediaId);
        model.put("parentPosting", parentPostingInfo);
        model.put("parentComment", parentCommentInfo);
    }

}
