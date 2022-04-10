package org.moera.node.liberin.model;

import org.moera.node.liberin.Liberin;
import org.moera.node.model.PostingInfo;

public class RemotePostingMediaReactionAddingFailedLiberin extends Liberin {

    private String postingId;
    private String parentPostingId;
    private String parentMediaId;
    private PostingInfo parentPostingInfo;

    public RemotePostingMediaReactionAddingFailedLiberin(String postingId, String parentPostingId, String parentMediaId,
                                                         PostingInfo parentPostingInfo) {
        this.postingId = postingId;
        this.parentPostingId = parentPostingId;
        this.parentMediaId = parentMediaId;
        this.parentPostingInfo = parentPostingInfo;
    }

    public String getPostingId() {
        return postingId;
    }

    public void setPostingId(String postingId) {
        this.postingId = postingId;
    }

    public String getParentPostingId() {
        return parentPostingId;
    }

    public void setParentPostingId(String parentPostingId) {
        this.parentPostingId = parentPostingId;
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

}
