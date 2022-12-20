package org.moera.node.data;

import java.sql.Timestamp;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.moera.node.auth.principal.Principal;
import org.moera.node.model.RemoteFeed;
import org.moera.node.model.RemotePosting;
import org.moera.node.option.Options;
import org.moera.node.util.Util;

@Entity
@Table(name = "user_subscriptions")
public class UserSubscription implements ContactRelated {

    @Id
    private UUID id;

    @NotNull
    private UUID nodeId;

    @NotNull
    @Enumerated
    private SubscriptionType subscriptionType;

    @Size(max = 63)
    private String feedName;

    @NotNull
    @Size(max = 63)
    private String remoteNodeName;

    @ManyToOne
    private Contact contact;

    @Size(max = 63)
    private String remoteFeedName;

    @Size(max = 40)
    private String remoteEntryId;

    @NotNull
    private Timestamp createdAt = Util.now();

    @NotNull
    @Enumerated
    private SubscriptionReason reason = SubscriptionReason.USER;

    @NotNull
    private Principal viewPrincipal = Principal.PUBLIC;

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

    @Override
    public Contact getContact() {
        return contact;
    }

    @Override
    public void setContact(Contact contact) {
        this.contact = contact;
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

    public SubscriptionReason getReason() {
        return reason;
    }

    public void setReason(SubscriptionReason reason) {
        this.reason = reason;
    }

    private Principal toAbsolute(Principal principal) {
        return principal.withOwner(getRemoteNodeName());
    }

    public static Principal getViewAllPrincipal(Options options) {
        return options.getPrincipal("subscriptions.view");
    }

    public static Principal getViewAllE(Options options) {
        return getViewAllPrincipal(options);
    }

    public static Principal getViewTotalPrincipal(Options options) {
        return options.getPrincipal("subscriptions.view-total");
    }

    public static Principal getViewTotalE(Options options) {
        return getViewTotalPrincipal(options);
    }

    public Principal getViewPrincipal() {
        return viewPrincipal;
    }

    public void setViewPrincipal(Principal viewPrincipal) {
        this.viewPrincipal = viewPrincipal;
    }

    public Principal getViewE() {
        return toAbsolute(getViewPrincipal());
    }

    @Override
    public void toContactViewPrincipal(Contact contact) {
        if (getSubscriptionType() == SubscriptionType.FEED) {
            contact.setViewFeedSubscriptionPrincipal(getViewPrincipal());
        }
    }

    public Principal getEditOperationsPrincipal() {
        return Principal.PRIVATE;
    }

    public Principal getEditOperationsE() {
        return toAbsolute(getEditOperationsPrincipal());
    }

    @Transient
    public RemoteFeed getRemoteFeed() {
        RemoteFeed remoteFeed = new RemoteFeed();
        remoteFeed.setNodeName(remoteNodeName);
        remoteFeed.setFeedName(remoteFeedName);
        return remoteFeed;
    }

    @Transient
    public RemotePosting getRemotePosting() {
        RemotePosting remotePosting = new RemotePosting();
        remotePosting.setNodeName(remoteNodeName);
        remotePosting.setPostingId(remoteEntryId);
        return remotePosting;
    }

}
