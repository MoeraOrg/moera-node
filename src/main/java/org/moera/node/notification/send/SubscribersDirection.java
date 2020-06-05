package org.moera.node.notification.send;

import java.util.UUID;

import org.moera.node.data.SubscriptionType;

public class SubscribersDirection extends Direction {

    private SubscriptionType subscriptionType;
    private String feedName;
    private UUID postingId;

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
