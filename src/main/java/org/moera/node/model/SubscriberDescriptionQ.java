package org.moera.node.model;

import java.time.Instant;

import org.moera.node.data.SubscriptionType;

public class SubscriberDescriptionQ {

    private SubscriptionType type;
    private String feedName;
    private String postingId;
    private String ownerFullName;
    private Long lastUpdatedAt;

    public SubscriberDescriptionQ() {
    }

    public SubscriberDescriptionQ(SubscriptionType type, String feedName, String postingId, String ownerFullName) {
        this(type, feedName, postingId, ownerFullName, Instant.now().getEpochSecond());
    }

    public SubscriberDescriptionQ(SubscriptionType type, String feedName, String postingId, String ownerFullName,
                                  Long lastUpdatedAt) {
        this.type = type;
        this.feedName = feedName;
        this.postingId = postingId;
        this.lastUpdatedAt = lastUpdatedAt;
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

    public String getPostingId() {
        return postingId;
    }

    public void setPostingId(String postingId) {
        this.postingId = postingId;
    }

    public String getOwnerFullName() {
        return ownerFullName;
    }

    public void setOwnerFullName(String ownerFullName) {
        this.ownerFullName = ownerFullName;
    }

    public Long getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(Long lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }

}
