package org.moera.node.model;

import java.util.HashMap;
import java.util.Map;

import org.moera.node.data.Reaction;
import org.moera.node.util.Util;

public class ReactionInfo {

    private String id;
    private String ownerName;
    private String postingId;
    private String postingRevisionId;
    private boolean negative;
    private int emoji;
    private long createdAt;
    private long deadline;
    private byte[] signature;
    private short signatureVersion;
    private Map<String, String[]> operations;

    public ReactionInfo() {
    }

    public ReactionInfo(Reaction reaction) {
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

    public boolean isNegative() {
        return negative;
    }

    public void setNegative(boolean negative) {
        this.negative = negative;
    }

    public int getEmoji() {
        return emoji;
    }

    public void setEmoji(int emoji) {
        this.emoji = emoji;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getDeadline() {
        return deadline;
    }

    public void setDeadline(long deadline) {
        this.deadline = deadline;
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

    public Map<String, String[]> getOperations() {
        return operations;
    }

    public void setOperations(Map<String, String[]> operations) {
        this.operations = operations;
    }

}
