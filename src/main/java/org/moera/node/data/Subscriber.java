package org.moera.node.data;

import java.sql.Timestamp;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.moera.node.auth.principal.Principal;
import org.moera.node.option.Options;
import org.moera.node.util.Util;

@Entity
@Table(name = "subscribers")
public class Subscriber implements ContactRelated {

    @Id
    private UUID id;

    @NotNull
    private UUID nodeId;

    @NotNull
    @Enumerated
    private SubscriptionType subscriptionType;

    @Size(max = 63)
    private String feedName;

    @ManyToOne
    private Entry entry;

    @NotNull
    @Size(max = 63)
    private String remoteNodeName;

    @ManyToOne
    private Contact contact;

    @NotNull
    private Timestamp createdAt = Util.now();

    @NotNull
    private Principal viewPrincipal = Principal.PUBLIC;

    @NotNull
    private Principal adminViewPrincipal = Principal.UNSET;

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

    public Entry getEntry() {
        return entry;
    }

    public void setEntry(Entry entry) {
        this.entry = entry;
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

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    private Principal toAbsolute(Principal principal) {
        return principal.withOwner(getRemoteNodeName());
    }

    public static Principal getViewAllPrincipal(Options options) {
        return options.getPrincipal("subscribers.view");
    }

    public static Principal getViewAllE(Options options) {
        return getViewAllPrincipal(options);
    }

    public static Principal getViewTotalPrincipal(Options options) {
        return options.getPrincipal("subscribers.view-total");
    }

    public static Principal getViewTotalE(Options options) {
        return getViewTotalPrincipal(options);
    }

    public static Principal getOverridePrincipal() {
        return Principal.ADMIN;
    }

    public static Principal getOverrideE() {
        return getOverridePrincipal();
    }

    public Principal getViewPrincipal() {
        return viewPrincipal;
    }

    public void setViewPrincipal(Principal viewPrincipal) {
        this.viewPrincipal = viewPrincipal;
    }

    public Principal getAdminViewPrincipal() {
        return adminViewPrincipal;
    }

    public void setAdminViewPrincipal(Principal adminViewPrincipal) {
        this.adminViewPrincipal = adminViewPrincipal;
    }

    public Principal getViewCompound() {
        return getAdminViewPrincipal().withSubordinate(getViewPrincipal());
    }

    public Principal getViewE() {
        return toAbsolute(getViewCompound());
    }

    @Override
    public void toContactViewPrincipal(Contact contact) {
        if (getSubscriptionType() == SubscriptionType.FEED) {
            contact.setViewFeedSubscriberPrincipal(getViewCompound());
        }
    }

    public Principal getViewOperationsPrincipal() {
        return Principal.PRIVATE;
    }

    public Principal getViewOperationsE() {
        return toAbsolute(getViewOperationsPrincipal());
    }

    public Principal getDeletePrincipal() {
        return Principal.PRIVATE;
    }

    public Principal getDeleteE() {
        return toAbsolute(getDeletePrincipal());
    }

}
