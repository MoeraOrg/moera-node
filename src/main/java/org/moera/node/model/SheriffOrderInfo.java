package org.moera.node.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.data.SheriffOrder;
import org.moera.node.util.Util;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SheriffOrderInfo {

    private String id;
    private boolean delete;
    private String sheriffName;
    private String nodeName;
    private String feedName;
    private String postingId;
    private String commentId;
    private SheriffOrderCategory category;
    private SheriffOrderReason reasonCode;
    private String reasonDetails;
    private long createdAt;
    private byte[] signature;
    private short signatureVersion;

    public SheriffOrderInfo() {
    }

    public SheriffOrderInfo(SheriffOrder sheriffOrder, String sheriffName) {
        id = sheriffOrder.getId().toString();
        delete = sheriffOrder.isDelete();
        this.sheriffName = sheriffName;
        nodeName = sheriffOrder.getRemoteNodeName();
        feedName = sheriffOrder.getRemoteFeedName();
        postingId = Objects.toString(sheriffOrder.getRemotePostingId(), null);
        commentId = Objects.toString(sheriffOrder.getRemoteCommentId(), null);
        category = sheriffOrder.getCategory();
        reasonCode = sheriffOrder.getReasonCode();
        reasonDetails = sheriffOrder.getReasonDetails();
        createdAt = Util.toEpochSecond(sheriffOrder.getCreatedAt());
        signature = sheriffOrder.getSignature();
        signatureVersion = sheriffOrder.getSignatureVersion();
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

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
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

}
