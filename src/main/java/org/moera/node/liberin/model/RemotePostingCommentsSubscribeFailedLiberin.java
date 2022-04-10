package org.moera.node.liberin.model;

import org.moera.node.liberin.Liberin;
import org.moera.node.model.PostingInfo;

public class RemotePostingCommentsSubscribeFailedLiberin extends Liberin {

    private String postingId;
    private PostingInfo postingInfo;

    public RemotePostingCommentsSubscribeFailedLiberin(String postingId, PostingInfo postingInfo) {
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

}
