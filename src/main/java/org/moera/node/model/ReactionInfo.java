package org.moera.node.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.data.Reaction;
import org.moera.node.data.ReactionTotal;
import org.moera.node.util.Util;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReactionInfo {

    private String id;
    private String ownerName;
    private String postingId;
    private String postingRevisionId;
    private Boolean negative;
    private Integer emoji;
    private Long createdAt;
    private Long deadline;
    private byte[] signature;
    private Short signatureVersion;
    private Map<String, String[]> operations;
    private ReactionTotalsInfo totals;

    public ReactionInfo() {
    }

    public ReactionInfo(Reaction reaction, Collection<ReactionTotal> totals) {
        id = reaction.getId().toString();
        ownerName = reaction.getOwnerName();
        postingId = reaction.getEntryRevision().getEntry().getId().toString();
        postingRevisionId = reaction.getEntryRevision().getId().toString();
        negative = reaction.isNegative();
        emoji = reaction.getEmoji();
        createdAt = Util.toEpochSecond(reaction.getCreatedAt());
        deadline = Util.toEpochSecond(reaction.getDeadline());
        signature = reaction.getSignature();
        signatureVersion = reaction.getSignatureVersion();
        operations = new HashMap<>();
        operations.put("delete", new String[]{"owner", "admin"});
        this.totals = new ReactionTotalsInfo(totals);
    }

    public ReactionInfo(UUID postingId, Collection<ReactionTotal> totals) {
        this.postingId = postingId.toString();
        this.totals = new ReactionTotalsInfo(totals);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public ReactionTotalsInfo getTotals() {
        return totals;
    }

    public void setTotals(ReactionTotalsInfo totals) {
        this.totals = totals;
    }

}
