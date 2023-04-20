package org.moera.node.data;

import java.sql.Timestamp;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

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
    private String remoteNodeName;

    @NotNull
    private String remoteFeedName;

    private String remotePostingId;

    private String remoteCommentId;

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

    public String getRemoteFeedName() {
        return remoteFeedName;
    }

    public void setRemoteFeedName(String remoteFeedName) {
        this.remoteFeedName = remoteFeedName;
    }

    public String getRemotePostingId() {
        return remotePostingId;
    }

    public void setRemotePostingId(String remotePostingId) {
        this.remotePostingId = remotePostingId;
    }

    public String getRemoteCommentId() {
        return remoteCommentId;
    }

    public void setRemoteCommentId(String remoteCommentId) {
        this.remoteCommentId = remoteCommentId;
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

}
