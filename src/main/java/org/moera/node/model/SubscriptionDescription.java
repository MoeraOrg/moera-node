package org.moera.node.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import org.moera.node.data.SubscriptionType;

public class SubscriptionDescription {

    private SubscriptionType type;

    @NotBlank
    private String feedName = "news";

    @NotBlank
    @Size(max = 40)
    private String remoteSubscriberId;

    @NotBlank
    @Size(max = 63)
    private String remoteNodeName;

    @Size(max = 63)
    private String remoteFeedName;

    @Size(max = 40)
    private String remotePostingId;

    public SubscriptionType getType() {
        return type;
    }

    public void setType(SubscriptionType type) {
        this.type = type;
    }

    public String getFeedName() {
        return feedName;
    }

    public void setFeedName(String feedName) {
        this.feedName = feedName;
    }

    public String getRemoteSubscriberId() {
        return remoteSubscriberId;
    }

    public void setRemoteSubscriberId(String remoteSubscriberId) {
        this.remoteSubscriberId = remoteSubscriberId;
    }

    public String getRemoteNodeName() {
        return remoteNodeName;
    }

    public void setRemoteNodeName(String remoteNodeName) {
        this.remoteNodeName = remoteNodeName;
    }

    public String getRemoteFeedName() {
        return remoteFeedName;
    }

    public void setRemoteFeedName(String remoteFeedName) {
        this.remoteFeedName = remoteFeedName;
    }

    public String getRemotePostingId() {
        return remotePostingId;
    }

    public void setRemotePostingId(String remotePostingId) {
        this.remotePostingId = remotePostingId;
    }

}
