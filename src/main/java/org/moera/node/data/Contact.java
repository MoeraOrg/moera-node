package org.moera.node.data;

import java.sql.Timestamp;
import java.util.Objects;
import java.util.UUID;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.model.AvatarImageUtil;
import org.moera.node.option.Options;
import org.moera.node.util.Util;

@Entity
@Table(name = "contacts")
public class Contact {

    @Id
    private UUID id;

    @NotNull
    private UUID nodeId;

    @NotNull
    @Size(max = 63)
    private String remoteNodeName = "";

    @Size(max = 96)
    private String remoteFullName;

    @Size(max = 31)
    private String remoteGender;

    @ManyToOne
    private MediaFile remoteAvatarMediaFile;

    @Size(max = 8)
    private String remoteAvatarShape;

    @NotNull
    private int feedSubscriptionCount;

    @NotNull
    private int feedSubscriberCount;

    @NotNull
    private int friendCount;

    @NotNull
    private int friendOfCount;

    @NotNull
    private int blockedUserCount;

    @NotNull
    private int blockedUserPostingCount;

    @NotNull
    private int blockedByUserCount;

    @NotNull
    private int blockedByUserPostingCount;

    @NotNull
    private float distance;

    @NotNull
    private Timestamp createdAt = Util.now();

    @NotNull
    private Timestamp updatedAt = Util.now();

    @NotNull
    private Principal viewFeedSubscriptionPrincipal = Principal.PUBLIC;

    @NotNull
    private Principal viewFeedSubscriberPrincipal = Principal.PUBLIC;

    @NotNull
    private Principal viewFriendPrincipal = Principal.PUBLIC;

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

    public String getRemoteFullName() {
        return remoteFullName;
    }

    public void setRemoteFullName(String remoteFullName) {
        this.remoteFullName = remoteFullName;
    }

    public String getRemoteGender() {
        return remoteGender;
    }

    public void setRemoteGender(String remoteGender) {
        this.remoteGender = remoteGender;
    }

    public MediaFile getRemoteAvatarMediaFile() {
        return remoteAvatarMediaFile;
    }

    public void setRemoteAvatarMediaFile(MediaFile remoteAvatarMediaFile) {
        this.remoteAvatarMediaFile = remoteAvatarMediaFile;
    }

    public String getRemoteAvatarShape() {
        return remoteAvatarShape;
    }

    public void setRemoteAvatarShape(String remoteAvatarShape) {
        this.remoteAvatarShape = remoteAvatarShape;
    }

    public int getFeedSubscriptionCount() {
        return feedSubscriptionCount;
    }

    public void setFeedSubscriptionCount(int feedSubscriptionCount) {
        this.feedSubscriptionCount = feedSubscriptionCount;
    }

    public int getFeedSubscriberCount() {
        return feedSubscriberCount;
    }

    public void setFeedSubscriberCount(int feedSubscriberCount) {
        this.feedSubscriberCount = feedSubscriberCount;
    }

    public int getFriendCount() {
        return friendCount;
    }

    public void setFriendCount(int friendCount) {
        this.friendCount = friendCount;
    }

    public int getFriendOfCount() {
        return friendOfCount;
    }

    public void setFriendOfCount(int friendOfCount) {
        this.friendOfCount = friendOfCount;
    }

    public int getBlockedUserCount() {
        return blockedUserCount;
    }

    public void setBlockedUserCount(int blockedUserCount) {
        this.blockedUserCount = blockedUserCount;
    }

    public int getBlockedUserPostingCount() {
        return blockedUserPostingCount;
    }

    public void setBlockedUserPostingCount(int blockedUserPostingCount) {
        this.blockedUserPostingCount = blockedUserPostingCount;
    }

    public int getBlockedByUserCount() {
        return blockedByUserCount;
    }

    public void setBlockedByUserCount(int blockedByUserCount) {
        this.blockedByUserCount = blockedByUserCount;
    }

    public int getBlockedByUserPostingCount() {
        return blockedByUserPostingCount;
    }

    public void setBlockedByUserPostingCount(int blockedByUserPostingCount) {
        this.blockedByUserPostingCount = blockedByUserPostingCount;
    }

    public float getDistance() {
        return distance;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    private Principal toAbsolute(Principal principal) {
        return principal.withOwner(getRemoteNodeName());
    }

    public Principal getViewFeedSubscriptionPrincipal() {
        return viewFeedSubscriptionPrincipal;
    }

    public void setViewFeedSubscriptionPrincipal(Principal viewFeedSubscriptionPrincipal) {
        this.viewFeedSubscriptionPrincipal = viewFeedSubscriptionPrincipal;
    }

    public Principal getViewFeedSubscriptionCompound(Options options) {
        return UserSubscription.getViewAllE(options).withSubordinate(getViewFeedSubscriptionPrincipal());
    }

    public Principal getViewFeedSubscriptionE(Options options) {
        return toAbsolute(getViewFeedSubscriptionCompound(options));
    }

    public Principal getViewFeedSubscriberPrincipal() {
        return viewFeedSubscriberPrincipal;
    }

    public void setViewFeedSubscriberPrincipal(Principal viewFeedSubscriberPrincipal) {
        this.viewFeedSubscriberPrincipal = viewFeedSubscriberPrincipal;
    }

    public Principal getViewFeedSubscriberCompound(Options options) {
        return Subscriber.getViewAllE(options).withSubordinate(getViewFeedSubscriberPrincipal());
    }

    public Principal getViewFeedSubscriberE(Options options) {
        return toAbsolute(getViewFeedSubscriberCompound(options));
    }

    public Principal getViewFriendPrincipal() {
        return viewFriendPrincipal;
    }

    public void setViewFriendPrincipal(Principal viewFriendPrincipal) {
        this.viewFriendPrincipal = viewFriendPrincipal;
    }

    public Principal getViewFriendCompound(Options options) {
        return Friend.getViewAllE(options).withSubordinate(getViewFriendPrincipal());
    }

    public Principal getViewFriendE(Options options) {
        return toAbsolute(getViewFriendCompound(options));
    }

    public void fill(ContactRelated related) {
        if (related != null) {
            related.setContact(this);
        }
    }

    public static void toAvatar(Contact contact, AvatarImage avatarImage) {
        if (
            contact != null && contact.getRemoteAvatarMediaFile() != null && avatarImage != null
            && Objects.equals(contact.getRemoteAvatarMediaFile().getId(), avatarImage.getMediaId())
        ) {
            AvatarImageUtil.setMediaFile(avatarImage, contact.getRemoteAvatarMediaFile());
        }
    }

}
