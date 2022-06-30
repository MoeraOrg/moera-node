package org.moera.node.model;

import java.time.Instant;
import java.util.Map;

import org.moera.node.auth.principal.Principal;
import org.moera.node.data.Avatar;
import org.moera.node.data.SubscriptionType;

public class SubscriberDescriptionQ {

    private SubscriptionType type;
    private String feedName;
    private String postingId;
    private String ownerFullName;
    private AvatarDescription ownerAvatar;
    private Long lastUpdatedAt;
    private Map<String, Principal> operations;

    public SubscriberDescriptionQ() {
    }

    public SubscriberDescriptionQ(SubscriptionType type, String feedName, String postingId, String ownerFullName,
                                  Avatar ownerAvatar) {
        this(type, feedName, postingId, ownerFullName, ownerAvatar, Instant.now().getEpochSecond());
    }

    public SubscriberDescriptionQ(SubscriptionType type, String feedName, String postingId, String ownerFullName,
                                  Avatar ownerAvatar, Long lastUpdatedAt) {
        this.type = type;
        this.feedName = feedName;
        this.postingId = postingId;
        this.ownerFullName = ownerFullName;
        if (ownerAvatar != null) {
            this.ownerAvatar = new AvatarDescription(ownerAvatar);
        }
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

    public AvatarDescription getOwnerAvatar() {
        return ownerAvatar;
    }

    public void setOwnerAvatar(AvatarDescription ownerAvatar) {
        this.ownerAvatar = ownerAvatar;
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
