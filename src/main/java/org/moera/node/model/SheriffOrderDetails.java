package org.moera.node.model;

import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.moera.node.data.MediaFile;

public class SheriffOrderDetails {

    @NotBlank
    @Size(max = 40)
    private String id;

    private boolean delete;

    @NotBlank
    @Size(max = 63)
    private String sheriffName;

    @Valid
    private AvatarDescription sheriffAvatar;

    @JsonIgnore
    private MediaFile sheriffAvatarMediaFile;

    @NotBlank
    @Size(max = 63)
    private String feedName;

    private UUID postingId;

    private UUID commentId;

    private SheriffOrderCategory category;

    private SheriffOrderReason reasonCode;

    @Size(max = 4096)
    private String reasonDetails;

    private long createdAt;

    private byte[] signature;

    private short signatureVersion;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isDelete() {
        return delete;
    }

    public void setDelete(boolean delete) {
        this.delete = delete;
    }

    public String getSheriffName() {
        return sheriffName;
    }

    public void setSheriffName(String sheriffName) {
        this.sheriffName = sheriffName;
    }

    public AvatarDescription getSheriffAvatar() {
        return sheriffAvatar;
    }

    public void setSheriffAvatar(AvatarDescription sheriffAvatar) {
        this.sheriffAvatar = sheriffAvatar;
    }

    public MediaFile getSheriffAvatarMediaFile() {
        return sheriffAvatarMediaFile;
    }

    public void setSheriffAvatarMediaFile(MediaFile sheriffAvatarMediaFile) {
        this.sheriffAvatarMediaFile = sheriffAvatarMediaFile;
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

    public UUID getCommentId() {
        return commentId;
    }

    public void setCommentId(UUID commentId) {
        this.commentId = commentId;
    }

    public SheriffOrderCategory getCategory() {
        return category;
    }

    public void setCategory(SheriffOrderCategory category) {
        this.category = category;
    }

    public SheriffOrderReason getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(SheriffOrderReason reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String getReasonDetails() {
        return reasonDetails;
    }

    public void setReasonDetails(String reasonDetails) {
        this.reasonDetails = reasonDetails;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public byte[] getSignature() {
        return signature;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    public short getSignatureVersion() {
        return signatureVersion;
    }

    public void setSignatureVersion(short signatureVersion) {
        this.signatureVersion = signatureVersion;
    }

}
