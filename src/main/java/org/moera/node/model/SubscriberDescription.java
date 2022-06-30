package org.moera.node.model;

import java.util.Map;
import java.util.UUID;
import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.moera.node.auth.principal.Principal;
import org.moera.node.data.MediaFile;
import org.moera.node.data.Subscriber;
import org.moera.node.data.SubscriptionType;

public class SubscriberDescription {

    private SubscriptionType type;

    private String feedName;

    private UUID postingId;

    private String ownerFullName;

    @Valid
    private AvatarDescription ownerAvatar;

    @JsonIgnore
    private MediaFile ownerAvatarMediaFile;

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

    public MediaFile getOwnerAvatarMediaFile() {
        return ownerAvatarMediaFile;
    }

    public void setOwnerAvatarMediaFile(MediaFile ownerAvatarMediaFile) {
        this.ownerAvatarMediaFile = ownerAvatarMediaFile;
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
        subscriber.setRemoteFullName(ownerFullName);
        if (ownerAvatar != null) {
            if (ownerAvatarMediaFile != null) {
                subscriber.setRemoteAvatarMediaFile(ownerAvatarMediaFile);
            }
            if (ownerAvatar.getShape() != null) {
                subscriber.setRemoteAvatarShape(ownerAvatar.getShape());
            }
        }
        subscriber.setFeedName(feedName);
        if (getPrincipal("view") != null) {
            subscriber.setViewPrincipal(getPrincipal("view"));
        }
    }

}
