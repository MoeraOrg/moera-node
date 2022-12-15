package org.moera.node.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.auth.principal.Principal;
import org.moera.node.data.SubscriptionReason;
import org.moera.node.data.SubscriptionType;
import org.moera.node.data.UserSubscription;
import org.moera.node.util.Util;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubscriptionInfo {

    private String id;
    private SubscriptionType type;
    private String feedName;
    private String remoteNodeName;
    private ContactInfo contact;
    private String remoteFeedName;
    private String remotePostingId;
    private Long createdAt;
    private SubscriptionReason reason;
    private Map<String, Principal> operations;

    public SubscriptionInfo() {
    }

    public SubscriptionInfo(UserSubscription subscription) {
        id = subscription.getId().toString();
        type = subscription.getSubscriptionType();
        feedName = subscription.getFeedName();
        remoteNodeName = subscription.getRemoteNodeName();
        if (subscription.getContact() != null) {
            contact = new ContactInfo(subscription.getContact());
        }
        remoteFeedName = subscription.getRemoteFeedName();
        remotePostingId = subscription.getRemoteEntryId();
        createdAt = Util.toEpochSecond(subscription.getCreatedAt());
        reason = subscription.getReason();

        operations = new HashMap<>();
        putOperation(operations, "view", subscription.getViewPrincipal(), Principal.PUBLIC);
    }

    private static void putOperation(Map<String, Principal> operations, String operationName, Principal value,
                                     Principal defaultValue) {
        if (value != null && !value.equals(defaultValue)) {
            operations.put(operationName, value);
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getRemoteNodeName() {
        return remoteNodeName;
    }

    public void setRemoteNodeName(String remoteNodeName) {
        this.remoteNodeName = remoteNodeName;
    }

    public ContactInfo getContact() {
        return contact;
    }

    public void setContact(ContactInfo contact) {
        this.contact = contact;
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

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
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

}
