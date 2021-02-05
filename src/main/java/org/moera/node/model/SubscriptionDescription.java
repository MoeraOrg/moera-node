package org.moera.node.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import org.moera.node.data.Subscription;
import org.moera.node.data.SubscriptionReason;
import org.moera.node.data.SubscriptionType;

public class SubscriptionDescription {

    private SubscriptionType type;

    private String feedName;

    @NotBlank
    @Size(max = 40)
    private String remoteSubscriberId;

    @NotBlank
    @Size(max = 63)
    private String remoteNodeName;

    @Size(max = 96)
    private String remoteFullName;

    @Size(max = 63)
    private String remoteFeedName;

    @Size(max = 40)
    private String remotePostingId;

    private SubscriptionReason reason = SubscriptionReason.USER;

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

    public String getRemoteFullName() {
        return remoteFullName;
    }

    public void setRemoteFullName(String remoteFullName) {
        this.remoteFullName = remoteFullName;
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

    public SubscriptionReason getReason() {
        return reason;
    }

    public void setReason(SubscriptionReason reason) {
        this.reason = reason;
    }

    public void toSubscription(Subscription subscription) {
        subscription.setSubscriptionType(type);
        subscription.setFeedName(feedName);
        subscription.setRemoteSubscriberId(remoteSubscriberId);
        subscription.setRemoteNodeName(remoteNodeName);
        subscription.setRemoteFullName(remoteFullName);
        subscription.setRemoteFeedName(remoteFeedName);
        subscription.setRemoteEntryId(remotePostingId);
        subscription.setReason(reason);
    }

}
