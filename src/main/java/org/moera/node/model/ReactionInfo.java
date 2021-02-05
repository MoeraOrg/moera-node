package org.moera.node.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.data.Entry;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.EntryType;
import org.moera.node.data.OwnReaction;
import org.moera.node.data.Reaction;
import org.moera.node.util.Util;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReactionInfo {

    private String ownerName;
    private String ownerFullName;
    private String postingId;
    private String postingRevisionId;
    private String commentId;
    private String commentRevisionId;
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
        ownerFullName = reaction.getOwnerFullName();
        EntryRevision entryRevision = reaction.getEntryRevision();
        Entry entry = entryRevision.getEntry();
        if (entry.getEntryType() == EntryType.POSTING) {
            postingId = entry.getId().toString();
            postingRevisionId = entryRevision.getId().toString();
        } else {
            commentId = entry.getId().toString();
            commentRevisionId = entryRevision.getId().toString();
            postingId = entry.getParent().getId().toString();
        }
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

    public static ReactionInfo ofPosting(UUID postingId) {
        ReactionInfo info = new ReactionInfo();
        info.setPostingId(postingId.toString());
        return info;
    }

    public static ReactionInfo ofComment(UUID commentId) {
        ReactionInfo info = new ReactionInfo();
        info.setCommentId(commentId.toString());
        return info;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getOwnerFullName() {
        return ownerFullName;
    }

    public void setOwnerFullName(String ownerFullName) {
        this.ownerFullName = ownerFullName;
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

    public void toOwnReaction(OwnReaction ownReaction) {
        ownReaction.setRemotePostingId(postingId);
        ownReaction.setNegative(negative);
        ownReaction.setEmoji(emoji);
        ownReaction.setCreatedAt(Util.now());
    }

}
