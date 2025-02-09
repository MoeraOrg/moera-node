package org.moera.node.data;

import java.sql.Timestamp;
import java.util.UUID;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.moera.node.model.CommentInfo;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.SheriffOrderCategory;
import org.moera.node.model.SheriffOrderReason;
import org.moera.node.util.Util;

@Entity
@Table(name = "sheriff_orders")
public class SheriffOrder {

    @Id
    private UUID id;

    @NotNull
    private UUID nodeId;

    @NotNull
    private boolean delete;

    @NotNull
    @Size(max = 63)
    private String remoteNodeName;

    @Size(max = 96)
    private String remoteNodeFullName;

    @NotNull
    @Size(max = 63)
    private String remoteFeedName;

    @Size(max = 63)
    private String remotePostingOwnerName;

    @Size(max = 96)
    private String remotePostingOwnerFullName;

    @Size(max = 31)
    private String remotePostingOwnerGender;

    @Size(max = 255)
    private String remotePostingHeading;

    @Size(max = 40)
    private String remotePostingId;

    @Size(max = 40)
    private String remotePostingRevisionId;

    @Size(max = 63)
    private String remoteCommentOwnerName;

    @Size(max = 96)
    private String remoteCommentOwnerFullName;

    @Size(max = 31)
    private String remoteCommentOwnerGender;

    @Size(max = 255)
    private String remoteCommentHeading;

    @Size(max = 40)
    private String remoteCommentId;

    @Size(max = 40)
    private String remoteCommentRevisionId;

    @NotNull
    @Enumerated
    private SheriffOrderCategory category;

    @NotNull
    @Enumerated
    private SheriffOrderReason reasonCode;

    private String reasonDetails;

    @NotNull
    private Timestamp createdAt = Util.now();

    @NotNull
    private byte[] signature;

    @NotNull
    private short signatureVersion;

    @ManyToOne
    private SheriffComplaintGroup complaintGroup;

    public SheriffOrder() {
    }

    public SheriffOrder(UUID id, UUID nodeId, String remoteNodeName) {
        this.id = id;
        this.nodeId = nodeId;
        this.remoteNodeName = remoteNodeName;
    }

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

    public boolean isDelete() {
        return delete;
    }

    public void setDelete(boolean delete) {
        this.delete = delete;
    }

    public String getRemoteNodeName() {
        return remoteNodeName;
    }

    public void setRemoteNodeName(String remoteNodeName) {
        this.remoteNodeName = remoteNodeName;
    }

    public String getRemoteNodeFullName() {
        return remoteNodeFullName;
    }

    public void setRemoteNodeFullName(String remoteNodeFullName) {
        this.remoteNodeFullName = remoteNodeFullName;
    }

    public String getRemoteFeedName() {
        return remoteFeedName;
    }

    public void setRemoteFeedName(String remoteFeedName) {
        this.remoteFeedName = remoteFeedName;
    }

    public String getRemotePostingOwnerName() {
        return remotePostingOwnerName;
    }

    public void setRemotePostingOwnerName(String remotePostingOwnerName) {
        this.remotePostingOwnerName = remotePostingOwnerName;
    }

    public String getRemotePostingOwnerFullName() {
        return remotePostingOwnerFullName;
    }

    public void setRemotePostingOwnerFullName(String remotePostingOwnerFullName) {
        this.remotePostingOwnerFullName = remotePostingOwnerFullName;
    }

    public String getRemotePostingOwnerGender() {
        return remotePostingOwnerGender;
    }

    public void setRemotePostingOwnerGender(String remotePostingOwnerGender) {
        this.remotePostingOwnerGender = remotePostingOwnerGender;
    }

    public String getRemotePostingHeading() {
        return remotePostingHeading;
    }

    public void setRemotePostingHeading(String remotePostingHeading) {
        this.remotePostingHeading = remotePostingHeading;
    }

    public String getRemotePostingId() {
        return remotePostingId;
    }

    public void setRemotePostingId(String remotePostingId) {
        this.remotePostingId = remotePostingId;
    }

    public String getRemotePostingRevisionId() {
        return remotePostingRevisionId;
    }

    public void setRemotePostingRevisionId(String remotePostingRevisionId) {
        this.remotePostingRevisionId = remotePostingRevisionId;
    }

    public void setRemotePosting(PostingInfo info) {
        remotePostingOwnerName = info.getOwnerName();
        remotePostingOwnerFullName = info.getOwnerFullName();
        remotePostingOwnerGender = info.getOwnerGender();
        remotePostingOwnerGender = info.getOwnerGender();
        remotePostingHeading = info.getHeading();
        remotePostingId = info.getId();
        remotePostingRevisionId = info.getRevisionId();
    }

    public String getRemoteCommentOwnerName() {
        return remoteCommentOwnerName;
    }

    public void setRemoteCommentOwnerName(String remoteCommentOwnerName) {
        this.remoteCommentOwnerName = remoteCommentOwnerName;
    }

    public String getRemoteCommentOwnerFullName() {
        return remoteCommentOwnerFullName;
    }

    public void setRemoteCommentOwnerFullName(String remoteCommentOwnerFullName) {
        this.remoteCommentOwnerFullName = remoteCommentOwnerFullName;
    }

    public String getRemoteCommentOwnerGender() {
        return remoteCommentOwnerGender;
    }

    public void setRemoteCommentOwnerGender(String remoteCommentOwnerGender) {
        this.remoteCommentOwnerGender = remoteCommentOwnerGender;
    }

    public String getRemoteCommentHeading() {
        return remoteCommentHeading;
    }

    public void setRemoteCommentHeading(String remoteCommentHeading) {
        this.remoteCommentHeading = remoteCommentHeading;
    }

    public String getRemoteCommentId() {
        return remoteCommentId;
    }

    public void setRemoteCommentId(String remoteCommentId) {
        this.remoteCommentId = remoteCommentId;
    }

    public String getRemoteCommentRevisionId() {
        return remoteCommentRevisionId;
    }

    public void setRemoteCommentRevisionId(String remoteCommentRevisionId) {
        this.remoteCommentRevisionId = remoteCommentRevisionId;
    }

    public void setRemoteComment(CommentInfo info) {
        remoteCommentOwnerName = info.getOwnerName();
        remoteCommentOwnerFullName = info.getOwnerFullName();
        remoteCommentOwnerGender = info.getOwnerGender();
        remoteCommentOwnerGender = info.getOwnerGender();
        remoteCommentHeading = info.getHeading();
        remoteCommentId = info.getId();
        remoteCommentRevisionId = info.getRevisionId();
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

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
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

    public SheriffComplaintGroup getComplaintGroup() {
        return complaintGroup;
    }

    public void setComplaintGroup(SheriffComplaintGroup complaintGroup) {
        this.complaintGroup = complaintGroup;
    }

}
