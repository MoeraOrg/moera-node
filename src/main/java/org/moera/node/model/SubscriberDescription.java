package org.moera.node.model;

import java.util.UUID;

import org.moera.node.data.SubscriptionType;

public class SubscriberDescription {

    private SubscriptionType type;
    private String feedName;
    private UUID postingId;

    public SubscriberDescription() {
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

    public UUID getPostingId() {
        return postingId;
    }

    public void setPostingId(UUID postingId) {
        this.postingId = postingId;
    }

}
