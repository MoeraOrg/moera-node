package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.data.SheriffOrder;
import org.moera.node.util.Util;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SheriffOrderInfo {

    private String id;
    private boolean delete;
    private String sheriffName;
    private String nodeName;
    private String nodeFullName;
    private String feedName;
    private String postingOwnerName;
    private String postingOwnerFullName;
    private String postingOwnerGender;
    private String postingHeading;
    private String postingId;
    private String postingRevisionId;
    private String commentOwnerName;
    private String commentOwnerFullName;
    private String commentOwnerGender;
    private String commentHeading;
    private String commentId;
    private String commentRevisionId;
    private SheriffOrderCategory category;
    private SheriffOrderReason reasonCode;
    private String reasonDetails;
    private long createdAt;
    private byte[] signature;
    private short signatureVersion;
    private String complainGroupId;

    public SheriffOrderInfo() {
    }

    public SheriffOrderInfo(SheriffOrder sheriffOrder, String sheriffName) {
        id = sheriffOrder.getId().toString();
        delete = sheriffOrder.isDelete();
        this.sheriffName = sheriffName;
        nodeName = sheriffOrder.getRemoteNodeName();
        nodeFullName = sheriffOrder.getRemoteNodeFullName();
        feedName = sheriffOrder.getRemoteFeedName();
        postingOwnerName = sheriffOrder.getRemotePostingOwnerName();
        postingOwnerFullName = sheriffOrder.getRemotePostingOwnerFullName();
        postingOwnerGender = sheriffOrder.getRemotePostingOwnerGender();
        postingHeading = sheriffOrder.getRemotePostingHeading();
        postingId = sheriffOrder.getRemotePostingId();
        postingRevisionId = sheriffOrder.getRemotePostingRevisionId();
        commentOwnerName = sheriffOrder.getRemoteCommentOwnerName();
        commentOwnerFullName = sheriffOrder.getRemoteCommentOwnerFullName();
        commentOwnerGender = sheriffOrder.getRemoteCommentOwnerGender();
        commentHeading = sheriffOrder.getRemoteCommentHeading();
        commentId = sheriffOrder.getRemoteCommentId();
        commentRevisionId = sheriffOrder.getRemoteCommentRevisionId();
        category = sheriffOrder.getCategory();
        reasonCode = sheriffOrder.getReasonCode();
        reasonDetails = sheriffOrder.getReasonDetails();
        createdAt = Util.toEpochSecond(sheriffOrder.getCreatedAt());
        signature = sheriffOrder.getSignature();
        signatureVersion = sheriffOrder.getSignatureVersion();
        if (sheriffOrder.getComplainGroup() != null) {
            complainGroupId = sheriffOrder.getComplainGroup().getId().toString();
        }
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

    public String getNodeFullName() {
        return nodeFullName;
    }

    public void setNodeFullName(String nodeFullName) {
        this.nodeFullName = nodeFullName;
    }

    public String getFeedName() {
        return feedName;
    }

    public void setFeedName(String feedName) {
        this.feedName = feedName;
    }

    public String getPostingOwnerName() {
        return postingOwnerName;
    }

    public void setPostingOwnerName(String postingOwnerName) {
        this.postingOwnerName = postingOwnerName;
    }

    public String getPostingOwnerFullName() {
        return postingOwnerFullName;
    }

    public void setPostingOwnerFullName(String postingOwnerFullName) {
        this.postingOwnerFullName = postingOwnerFullName;
    }

    public String getPostingOwnerGender() {
        return postingOwnerGender;
    }

    public void setPostingOwnerGender(String postingOwnerGender) {
        this.postingOwnerGender = postingOwnerGender;
    }

    public String getPostingHeading() {
        return postingHeading;
    }

    public void setPostingHeading(String postingHeading) {
        this.postingHeading = postingHeading;
    }

    public String getPostingId() {
        return postingId;
    }

    public void setPostingId(String postingId) {
        this.postingId = postingId;
    }

    public String getPostingRevisionId() {
        return postingRevisionId;
    }

    public void setPostingRevisionId(String postingRevisionId) {
        this.postingRevisionId = postingRevisionId;
    }

    public String getCommentOwnerName() {
        return commentOwnerName;
    }

    public void setCommentOwnerName(String commentOwnerName) {
        this.commentOwnerName = commentOwnerName;
    }

    public String getCommentOwnerFullName() {
        return commentOwnerFullName;
    }

    public void setCommentOwnerFullName(String commentOwnerFullName) {
        this.commentOwnerFullName = commentOwnerFullName;
    }

    public String getCommentOwnerGender() {
        return commentOwnerGender;
    }

    public void setCommentOwnerGender(String commentOwnerGender) {
        this.commentOwnerGender = commentOwnerGender;
    }

    public String getCommentHeading() {
        return commentHeading;
    }

    public void setCommentHeading(String commentHeading) {
        this.commentHeading = commentHeading;
    }

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public String getCommentRevisionId() {
        return commentRevisionId;
    }

    public void setCommentRevisionId(String commentRevisionId) {
        this.commentRevisionId = commentRevisionId;
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

    public String getComplainGroupId() {
        return complainGroupId;
    }

    public void setComplainGroupId(String complainGroupId) {
        this.complainGroupId = complainGroupId;
    }

}
