package org.moera.node.model;

import java.util.Map;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import org.moera.lib.naming.NodeName;
import org.moera.node.auth.principal.Principal;
import org.moera.node.data.SubscriptionReason;
import org.moera.node.data.SubscriptionType;
import org.moera.node.data.UserSubscription;

public class SubscriptionDescription {

    private SubscriptionType type;

    private String feedName;

    @NotBlank
    @Size(max = 63)
    private String remoteNodeName;

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

    public String getRemoteNodeName() {
        return remoteNodeName;
    }

    public void setRemoteNodeName(String remoteNodeName) {
        this.remoteNodeName = remoteNodeName;
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

    public void toUserSubscription(UserSubscription subscription) {
        subscription.setSubscriptionType(type);
        subscription.setFeedName(feedName);
        subscription.setRemoteNodeName(NodeName.expand(remoteNodeName));
        subscription.setRemoteFeedName(remoteFeedName);
        subscription.setRemoteEntryId(remotePostingId);
        subscription.setReason(reason);
        if (getPrincipal("view") != null) {
            subscription.setViewPrincipal(getPrincipal("view"));
        }
    }

}
