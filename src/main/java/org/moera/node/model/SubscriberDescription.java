package org.moera.node.model;

import java.util.Map;
import java.util.UUID;
import javax.validation.constraints.Size;

import org.moera.node.auth.principal.Principal;
import org.moera.node.data.Subscriber;
import org.moera.node.data.SubscriptionType;

public class SubscriberDescription {

    private SubscriptionType type;

    @Size(max = 63)
    private String feedName;

    private UUID postingId;

    private Long lastUpdatedAt;

    private Map<String, Principal> operations;

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

    public Principal getPrincipal(String operationName) {
        return operations != null ? operations.get(operationName) : null;
    }

    public void toSubscriber(Subscriber subscriber) {
        subscriber.setSubscriptionType(type);
        subscriber.setFeedName(feedName);
        if (getPrincipal("view") != null) {
            subscriber.setViewPrincipal(getPrincipal("view"));
        }
    }

}
