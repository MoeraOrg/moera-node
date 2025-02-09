package org.moera.node.data;

import java.sql.Timestamp;
import java.util.UUID;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.moera.node.model.SheriffOrderReason;
import org.moera.node.util.Util;

@Entity
@Table(name = "sheriff_complaint_groups")
public class SheriffComplaintGroup {

    @Id
    private UUID id;

    @NotNull
    private UUID nodeId;

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
    private Timestamp createdAt = Util.now();

    @NotNull
    private long moment;

    @NotNull
    @Enumerated
    private SheriffComplaintStatus status = SheriffComplaintStatus.POSTED;

    @Enumerated
    private SheriffOrderReason decisionCode;

    private String decisionDetails;

    private Timestamp decidedAt;

    @NotNull
    private boolean anonymous;

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

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public long getMoment() {
        return moment;
    }

    public void setMoment(long moment) {
        this.moment = moment;
    }

    public SheriffComplaintStatus getStatus() {
        return status;
    }

    public void setStatus(SheriffComplaintStatus status) {
        this.status = status;
    }

    public SheriffOrderReason getDecisionCode() {
        return decisionCode;
    }

    public void setDecisionCode(SheriffOrderReason decisionCode) {
        this.decisionCode = decisionCode;
    }

    public String getDecisionDetails() {
        return decisionDetails;
    }

    public void setDecisionDetails(String decisionDetails) {
        this.decisionDetails = decisionDetails;
    }

    public Timestamp getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(Timestamp decidedAt) {
        this.decidedAt = decidedAt;
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    public void setAnonymous(boolean anonymous) {
        this.anonymous = anonymous;
    }

}
