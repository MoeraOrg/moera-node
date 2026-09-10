package org.moera.node.liberin.model;

import org.moera.lib.node.types.PostingInfo;
import org.moera.node.liberin.Liberin;

public class RemoteCommentAddingFailedLiberin extends Liberin {

    private String remoteNodeName;
    private String remotePostingId;
    private PostingInfo postingInfo;

    public RemoteCommentAddingFailedLiberin(String remoteNodeName, String remotePostingId, PostingInfo postingInfo) {
        this.remoteNodeName = remoteNodeName;
        this.remotePostingId = remotePostingId;
        this.postingInfo = postingInfo;
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

}
