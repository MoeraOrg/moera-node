package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.node.liberin.Liberin;
import org.moera.node.model.PostingInfo;

public class RemoteCommentAddingFailedLiberin extends Liberin {

    private String postingId;
    private PostingInfo postingInfo;

    public RemoteCommentAddingFailedLiberin(String postingId, PostingInfo postingInfo) {
        this.postingId = postingId;
        this.postingInfo = postingInfo;
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

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("postingId", postingId);
        model.put("posting", postingInfo);
    }

}
