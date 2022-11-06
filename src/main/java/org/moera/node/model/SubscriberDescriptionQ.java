package org.moera.node.model;

import java.util.Collections;
import java.util.Map;

import org.moera.node.auth.principal.Principal;
import org.moera.node.data.Avatar;
import org.moera.node.data.SubscriptionType;

public class SubscriberDescriptionQ {

    private SubscriptionType type;
    private String feedName;
    private String postingId;
    private String ownerFullName;
    private String ownerGender;
    private AvatarDescription ownerAvatar;
    private Long lastUpdatedAt;
    private Map<String, Principal> operations;

    public SubscriberDescriptionQ() {
    }

    public SubscriberDescriptionQ(SubscriptionType type, String feedName, String postingId, String ownerFullName,
                                  String ownerGender, Avatar ownerAvatar, Long lastUpdatedAt, boolean visible) {
        this.type = type;
        this.feedName = feedName;
        this.postingId = postingId;
        this.ownerFullName = ownerFullName;
        this.ownerGender = ownerGender;
        if (ownerAvatar != null) {
            this.ownerAvatar = new AvatarDescription(ownerAvatar);
        }
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

    public String getOwnerFullName() {
        return ownerFullName;
    }

    public void setOwnerFullName(String ownerFullName) {
        this.ownerFullName = ownerFullName;
    }

    public String getOwnerGender() {
        return ownerGender;
    }

    public void setOwnerGender(String ownerGender) {
        this.ownerGender = ownerGender;
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
