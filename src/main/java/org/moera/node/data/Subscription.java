package org.moera.node.data;

import java.sql.Timestamp;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.moera.node.util.Util;

@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    private UUID id;

    @NotNull
    private UUID nodeId;

    @NotNull
    @Enumerated
    private SubscriptionType subscriptionType;

    @Size(max = 40)
    private String remoteSubscriberId;

    @NotNull
    @Size(max = 63)
    private String remoteNodeName;

    @Size(max = 63)
    private String remoteFeedName;

    @Size(max = 40)
    private String remoteEntryId;

    @NotNull
    private Timestamp createdAt = Util.now();

    @NotNull
    @Enumerated
    private SubscriptionStatus status;

    private Timestamp retryAt;

    @NotNull
    @Column(insertable = false, updatable = false)
    private int usageCount;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
    }

    public SubscriptionType getSubscriptionType() {
        return subscriptionType;
    }

    public void setSubscriptionType(SubscriptionType subscriptionType) {
        this.subscriptionType = subscriptionType;
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

    public String getRemoteFeedName() {
        return remoteFeedName;
    }

    public void setRemoteFeedName(String remoteFeedName) {
        this.remoteFeedName = remoteFeedName;
    }

    public String getRemoteEntryId() {
        return remoteEntryId;
    }

    public void setRemoteEntryId(String remoteEntryId) {
        this.remoteEntryId = remoteEntryId;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public void setStatus(SubscriptionStatus status) {
        this.status = status;
    }

    public Timestamp getRetryAt() {
        return retryAt;
    }

    public void setRetryAt(Timestamp retryAt) {
        this.retryAt = retryAt;
    }

    public int getUsageCount() {
        return usageCount;
    }

}
