package org.moera.node.data;

import java.sql.Timestamp;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.moera.node.auth.principal.Principal;
import org.moera.node.option.Options;
import org.moera.node.util.Util;

@Entity
@Table(name = "friends")
public class Friend implements ContactRelated {

    @Id
    private UUID id;

    @NotNull
    private UUID nodeId;

    @NotNull
    @Size(max = 63)
    private String remoteNodeName;

    @Size(max = 96)
    private String remoteFullName;

    @Size(max = 31)
    private String remoteGender;

    @ManyToOne
    private MediaFile remoteAvatarMediaFile;

    @Size(max = 8)
    private String remoteAvatarShape;

    @ManyToOne
    private Contact contact;

    @ManyToOne
    private FriendGroup friendGroup;

    @NotNull
    private Timestamp createdAt = Util.now();

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

    public String getRemoteNodeName() {
        return remoteNodeName;
    }

    public void setRemoteNodeName(String remoteNodeName) {
        this.remoteNodeName = remoteNodeName;
    }

    @Override
    public String getRemoteFullName() {
        return remoteFullName;
    }

    @Override
    public void setRemoteFullName(String remoteFullName) {
        this.remoteFullName = remoteFullName;
    }

    @Override
    public String getRemoteGender() {
        return remoteGender;
    }

    @Override
    public void setRemoteGender(String remoteGender) {
        this.remoteGender = remoteGender;
    }

    @Override
    public MediaFile getRemoteAvatarMediaFile() {
        return remoteAvatarMediaFile;
    }

    @Override
    public void setRemoteAvatarMediaFile(MediaFile remoteAvatarMediaFile) {
        this.remoteAvatarMediaFile = remoteAvatarMediaFile;
    }

    @Override
    public String getRemoteAvatarShape() {
        return remoteAvatarShape;
    }

    @Override
    public void setRemoteAvatarShape(String remoteAvatarShape) {
        this.remoteAvatarShape = remoteAvatarShape;
    }

    @Override
    public Contact getContact() {
        return contact;
    }

    @Override
    public void setContact(Contact contact) {
        this.contact = contact;
    }

    public FriendGroup getFriendGroup() {
        return friendGroup;
    }

    public void setFriendGroup(FriendGroup friendGroup) {
        this.friendGroup = friendGroup;
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
        return options.getPrincipal("friends.view");
    }

    public static Principal getViewAllE(Options options) {
        return getViewAllPrincipal(options);
    }

    public Principal getViewE() {
        return toAbsolute(getViewPrincipal());
    }

    public Principal getViewPrincipal() {
        return viewPrincipal;
    }

    public void setViewPrincipal(Principal viewPrincipal) {
        this.viewPrincipal = viewPrincipal;
    }

}
