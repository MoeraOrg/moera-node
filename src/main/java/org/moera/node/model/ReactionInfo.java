package org.moera.node.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.data.Reaction;
import org.moera.node.util.Util;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReactionInfo {

    private String ownerName;
    private String postingId;
    private String postingRevisionId;
    private Boolean negative;
    private Integer emoji;
    private long moment;
    private Long createdAt;
    private Long deadline;
    private byte[] signature;
    private Short signatureVersion;
    private Map<String, String[]> operations;

    public ReactionInfo() {
    }

    public ReactionInfo(Reaction reaction) {
        ownerName = reaction.getOwnerName();
        postingId = reaction.getEntryRevision().getEntry().getId().toString();
        postingRevisionId = reaction.getEntryRevision().getId().toString();
        negative = reaction.isNegative();
        emoji = reaction.getEmoji();
        moment = reaction.getMoment();
        createdAt = Util.toEpochSecond(reaction.getCreatedAt());
        deadline = Util.toEpochSecond(reaction.getDeadline());
        signature = reaction.getSignature();
        signatureVersion = reaction.getSignatureVersion();
        operations = new HashMap<>();
        operations.put("delete", new String[]{"owner", "admin"});
    }

    public ReactionInfo(UUID postingId) {
        this.postingId = postingId.toString();
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
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

    public Boolean isNegative() {
        return negative;
    }

    public void setNegative(Boolean negative) {
        this.negative = negative;
    }

    public Integer getEmoji() {
        return emoji;
    }

    public void setEmoji(Integer emoji) {
        this.emoji = emoji;
    }

    public long getMoment() {
        return moment;
    }

    public void setMoment(long moment) {
        this.moment = moment;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getDeadline() {
        return deadline;
    }

    public void setDeadline(Long deadline) {
        this.deadline = deadline;
    }

    public byte[] getSignature() {
        return signature;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    public Short getSignatureVersion() {
        return signatureVersion;
    }

    public void setSignatureVersion(Short signatureVersion) {
        this.signatureVersion = signatureVersion;
    }

    public Map<String, String[]> getOperations() {
        return operations;
    }

    public void setOperations(Map<String, String[]> operations) {
        this.operations = operations;
    }

}
