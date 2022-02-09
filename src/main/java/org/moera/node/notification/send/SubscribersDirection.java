package org.moera.node.notification.send;

import java.util.UUID;

import org.moera.node.data.SubscriptionType;

class SubscribersDirection extends Direction {

    private SubscriptionType subscriptionType;
    private String feedName;
    private UUID postingId;

    SubscribersDirection(UUID nodeId, SubscriptionType subscriptionType) {
        super(nodeId);
        this.subscriptionType = subscriptionType;
    }

    SubscribersDirection(UUID nodeId, SubscriptionType subscriptionType, String feedName) {
        super(nodeId);
        this.subscriptionType = subscriptionType;
        this.feedName = feedName;
    }

    SubscribersDirection(UUID nodeId, SubscriptionType subscriptionType, UUID postingId) {
        super(nodeId);
        this.subscriptionType = subscriptionType;
        this.postingId = postingId;
    }

    public SubscriptionType getSubscriptionType() {
        return subscriptionType;
    }

    public void setSubscriptionType(SubscriptionType subscriptionType) {
        this.subscriptionType = subscriptionType;
    }

    public String getFeedName() {
        return feedName;
    }

    public void setFeedName(String feedName) {
        this.feedName = feedName;
    }

    public UUID getPostingId() {
        return postingId;
    }

    public void setPostingId(UUID postingId) {
        this.postingId = postingId;
    }

}
