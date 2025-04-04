package org.moera.node.data;

import java.sql.Timestamp;
import java.util.UUID;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.moera.lib.node.types.principal.Principal;
import org.moera.node.option.Options;
import org.moera.node.util.Util;

@Entity
@Table(name = "friend_ofs")
public class FriendOf implements ContactRelated {

    @Id
    private UUID id;

    @NotNull
    private UUID nodeId;

    @NotNull
    @Size(max = 63)
    private String remoteNodeName;

    @ManyToOne
    private Contact contact;

    @NotNull
    @Size(max = 40)
    private String remoteGroupId;

    @Size(max = 63)
    private String remoteGroupTitle;

    @NotNull
    private Timestamp remoteAddedAt;

    @NotNull
    private Timestamp createdAt = Util.now();

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

    public String getRemoteGroupId() {
        return remoteGroupId;
    }

    public void setRemoteGroupId(String remoteGroupId) {
        this.remoteGroupId = remoteGroupId;
    }

    public String getRemoteGroupTitle() {
        return remoteGroupTitle;
    }

    public void setRemoteGroupTitle(String remoteGroupTitle) {
        this.remoteGroupTitle = remoteGroupTitle;
    }

    public Timestamp getRemoteAddedAt() {
        return remoteAddedAt;
    }

    public void setRemoteAddedAt(Timestamp remoteAddedAt) {
        this.remoteAddedAt = remoteAddedAt;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public static Principal getViewAllPrincipal(Options options) {
        return options.getPrincipal("friend-ofs.view");
    }

    public static Principal getViewAllE(Options options) {
        return getViewAllPrincipal(options);
    }

    public static Principal getViewTotalPrincipal(Options options) {
        return options.getPrincipal("friend-ofs.view-total");
    }

    public static Principal getViewTotalE(Options options) {
        return getViewTotalPrincipal(options);
    }

    @Override
    public void toContactViewPrincipal(Contact contact) {
    }

}
