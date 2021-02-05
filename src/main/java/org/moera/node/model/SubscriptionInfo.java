package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.commons.util.Util;
import org.moera.node.data.Subscription;
import org.moera.node.data.SubscriptionReason;
import org.moera.node.data.SubscriptionType;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubscriptionInfo {

    private String id;
    private SubscriptionType type;
    private String feedName;
    private String remoteSubscriberId;
    private String remoteNodeName;
    private String remoteFullName;
    private String remoteFeedName;
    private String remotePostingId;
    private Long createdAt;
    private SubscriptionReason reason;

    public SubscriptionInfo() {
    }

    public SubscriptionInfo(Subscription subscription) {
        id = subscription.getId().toString();
        type = subscription.getSubscriptionType();
        feedName = subscription.getFeedName();
        remoteSubscriberId = subscription.getRemoteSubscriberId();
        remoteNodeName = subscription.getRemoteNodeName();
        remoteFullName = subscription.getRemoteFullName();
        remoteFeedName = subscription.getRemoteFeedName();
        remotePostingId = subscription.getRemoteEntryId();
        createdAt = Util.toEpochSecond(subscription.getCreatedAt());
        reason = subscription.getReason();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public SubscriptionReason getReason() {
        return reason;
    }

    public void setReason(SubscriptionReason reason) {
        this.reason = reason;
    }

}
