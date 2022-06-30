package org.moera.node.model;

import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.moera.node.auth.principal.Principal;
import org.moera.node.data.MediaFile;
import org.moera.node.data.Subscription;
import org.moera.node.data.SubscriptionReason;
import org.moera.node.data.SubscriptionType;

public class SubscriptionDescription {

    private SubscriptionType type;

    private String feedName;

    @NotBlank
    @Size(max = 40)
    private String remoteSubscriberId;

    @NotBlank
    @Size(max = 63)
    private String remoteNodeName;

    @Size(max = 96)
    private String remoteFullName;

    @Valid
    private AvatarDescription remoteAvatar;

    @JsonIgnore
    private MediaFile remoteAvatarMediaFile;

    @Size(max = 63)
    private String remoteFeedName;

    @Size(max = 40)
    private String remotePostingId;

    private SubscriptionReason reason = SubscriptionReason.USER;

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

    public AvatarDescription getRemoteAvatar() {
        return remoteAvatar;
    }

    public void setRemoteAvatar(AvatarDescription remoteAvatar) {
        this.remoteAvatar = remoteAvatar;
    }

    public MediaFile getRemoteAvatarMediaFile() {
        return remoteAvatarMediaFile;
    }

    public void setRemoteAvatarMediaFile(MediaFile remoteAvatarMediaFile) {
        this.remoteAvatarMediaFile = remoteAvatarMediaFile;
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

    public SubscriptionReason getReason() {
        return reason;
    }

    public void setReason(SubscriptionReason reason) {
        this.reason = reason;
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

    public void toSubscription(Subscription subscription) {
        subscription.setSubscriptionType(type);
        subscription.setFeedName(feedName);
        subscription.setRemoteSubscriberId(remoteSubscriberId);
        subscription.setRemoteNodeName(remoteNodeName);
        subscription.setRemoteFullName(remoteFullName);
        if (remoteAvatar != null) {
            if (remoteAvatarMediaFile != null) {
                subscription.setRemoteAvatarMediaFile(remoteAvatarMediaFile);
            }
            if (remoteAvatar.getShape() != null) {
                subscription.setRemoteAvatarShape(remoteAvatar.getShape());
            }
        }
        subscription.setRemoteFeedName(remoteFeedName);
        subscription.setRemoteEntryId(remotePostingId);
        subscription.setReason(reason);
        if (getPrincipal("view") != null) {
            subscription.setViewPrincipal(getPrincipal("view"));
        }
    }

}
