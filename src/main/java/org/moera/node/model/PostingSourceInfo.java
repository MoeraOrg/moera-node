package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.commons.util.Util;
import org.moera.node.data.EntrySource;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostingSourceInfo {

    private String nodeName;
    private String feedName;
    private String postingId;
    private Long createdAt;

    public PostingSourceInfo() {
    }

    public PostingSourceInfo(EntrySource entrySource) {
        nodeName = entrySource.getRemoteNodeName();
        feedName = entrySource.getRemoteFeedName();
        postingId = entrySource.getRemotePostingId();
        createdAt = Util.toEpochSecond(entrySource.getCreatedAt());
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getFeedName() {
        return feedName;
    }

    public void setFeedName(String feedName) {
        this.feedName = feedName;
    }

    public String getPostingId() {
        return postingId;
    }

    public void setPostingId(String postingId) {
        this.postingId = postingId;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

}
