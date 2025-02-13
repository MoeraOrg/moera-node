package org.moera.node.model;

import org.moera.lib.node.types.AvatarDescription;
import org.moera.lib.node.types.SheriffOrderCategory;
import org.moera.lib.node.types.SheriffOrderReason;
import org.moera.node.data.SheriffOrder;
import org.moera.node.util.Util;

public class SheriffOrderDetailsQ {

    private String id;
    private boolean delete;
    private String sheriffName;
    private AvatarDescription sheriffAvatar;
    private String feedName;
    private String postingId;
    private String commentId;
    private SheriffOrderCategory category;
    private SheriffOrderReason reasonCode;
    private String reasonDetails;
    private long createdAt;
    private byte[] signature;
    private short signatureVersion;

    public SheriffOrderDetailsQ() {
    }

    public SheriffOrderDetailsQ(String id, String sheriffName, AvatarDescription sheriffAvatar,
                                SheriffOrderAttributes attributes) {
        this.id = id;
        delete = attributes.isDelete();
        this.sheriffName = sheriffName;
        this.sheriffAvatar = sheriffAvatar;
        feedName = attributes.getFeedName();
        postingId = attributes.getPostingId();
        commentId = attributes.getCommentId();
        category = attributes.getCategory();
        reasonCode = attributes.getReasonCode();
        reasonDetails = attributes.getReasonDetails();
    }

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

    public String getFeedName() {
        return feedName;
    }

    public void setFeedName(String feedName) {
        this.feedName = feedName;
    }

    public String getPostingId() {
        return postingId;
    }

    public void setPostingId(String postingId) {
        this.postingId = postingId;
    }

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
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

    public void toSheriffOrder(SheriffOrder sheriffOrder) {
        sheriffOrder.setDelete(delete);
        sheriffOrder.setRemoteFeedName(feedName);
        sheriffOrder.setCategory(category);
        sheriffOrder.setReasonCode(reasonCode);
        sheriffOrder.setReasonDetails(reasonDetails);
        sheriffOrder.setCreatedAt(Util.toTimestamp(createdAt));
        sheriffOrder.setSignature(signature);
        sheriffOrder.setSignatureVersion(signatureVersion);
    }

}
