package org.moera.node.model;

import java.util.Collections;
import java.util.Map;

import org.moera.lib.node.types.SubscriptionType;
import org.moera.node.auth.principal.Principal;

public class SubscriberDescriptionQ {

    private SubscriptionType type;
    private String feedName;
    private String postingId;
    private Long lastUpdatedAt;
    private Map<String, Principal> operations;

    public SubscriberDescriptionQ() {
    }

    public SubscriberDescriptionQ(
        SubscriptionType type, String feedName, String postingId, Long lastUpdatedAt, boolean visible
    ) {
        this.type = type;
        this.feedName = feedName;
        this.postingId = postingId;
        this.lastUpdatedAt = lastUpdatedAt;
        operations = Collections.singletonMap("view", visible ? Principal.PUBLIC : Principal.PRIVATE);
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

    public Long getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(Long lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public Map<String, Principal> getOperations() {
        return operations;
    }

    public void setOperations(Map<String, Principal> operations) {
        this.operations = operations;
    }

}
