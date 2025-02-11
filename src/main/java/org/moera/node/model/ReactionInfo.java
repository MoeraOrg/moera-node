package org.moera.node.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.lib.node.types.Scope;
import org.moera.node.auth.principal.AccessChecker;
import org.moera.node.auth.principal.Principal;
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
    private String ownerGender;
    private AvatarImage ownerAvatar;
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
    private Map<String, Principal> operations;
    private Map<String, Principal> ownerOperations;
    private Map<String, Principal> seniorOperations;
    private Map<String, Principal> majorOperations;

    public ReactionInfo() {
    }

    public ReactionInfo(Reaction reaction, AccessChecker accessChecker) {
        ownerName = reaction.getOwnerName();
        ownerFullName = reaction.getOwnerFullName();
        ownerGender = reaction.getOwnerGender();
        if (reaction.getOwnerAvatarMediaFile() != null) {
            ownerAvatar = new AvatarImage(reaction.getOwnerAvatarMediaFile(), reaction.getOwnerAvatarShape());
        }
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
        putOperation(operations, "view", reaction.getViewCompound(), Principal.PUBLIC);
        putOperation(operations, "delete", reaction.getDeleteCompound(), Principal.PRIVATE);
        if (accessChecker.isPrincipal(reaction.getViewOperationsE(), Scope.VIEW_CONTENT)) {
            ownerOperations = new HashMap<>();
            putOperation(ownerOperations, "view", reaction.getViewPrincipal(), Principal.PUBLIC);
            putOperation(ownerOperations, "delete", reaction.getDeletePrincipal(), Principal.PRIVATE);
            if (reaction.getEntryRevision().getEntry().getParent() == null) {
                seniorOperations = new HashMap<>();
                putOperation(seniorOperations, "view", reaction.getPostingViewPrincipal(), Principal.UNSET);
                putOperation(seniorOperations, "delete", reaction.getPostingViewPrincipal(), Principal.UNSET);
            } else {
                seniorOperations = new HashMap<>();
                putOperation(seniorOperations, "view", reaction.getCommentViewPrincipal(), Principal.UNSET);
                putOperation(seniorOperations, "delete", reaction.getCommentDeletePrincipal(), Principal.UNSET);
                majorOperations = new HashMap<>();
                putOperation(majorOperations, "view", reaction.getPostingViewPrincipal(), Principal.UNSET);
                putOperation(majorOperations, "delete", reaction.getPostingDeletePrincipal(), Principal.UNSET);
            }
        }
    }

    private static void putOperation(Map<String, Principal> operations, String operationName, Principal value,
                                     Principal defaultValue) {
        if (value != null && !value.equals(defaultValue)) {
            operations.put(operationName, value);
        }
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

    public String getOwnerGender() {
        return ownerGender;
    }

    public void setOwnerGender(String ownerGender) {
        this.ownerGender = ownerGender;
    }

    public AvatarImage getOwnerAvatar() {
        return ownerAvatar;
    }

    public void setOwnerAvatar(AvatarImage ownerAvatar) {
        this.ownerAvatar = ownerAvatar;
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

    public Map<String, Principal> getOperations() {
        return operations;
    }

    public void setOperations(Map<String, Principal> operations) {
        this.operations = operations;
    }

    public Map<String, Principal> getOwnerOperations() {
        return ownerOperations;
    }

    public void setOwnerOperations(Map<String, Principal> ownerOperations) {
        this.ownerOperations = ownerOperations;
    }

    public Map<String, Principal> getSeniorOperations() {
        return seniorOperations;
    }

    public void setSeniorOperations(Map<String, Principal> seniorOperations) {
        this.seniorOperations = seniorOperations;
    }

    public Map<String, Principal> getMajorOperations() {
        return majorOperations;
    }

    public void setMajorOperations(Map<String, Principal> majorOperations) {
        this.majorOperations = majorOperations;
    }

    public void toOwnReaction(OwnReaction ownReaction) {
        ownReaction.setRemotePostingId(postingId);
        ownReaction.setNegative(negative);
        ownReaction.setEmoji(emoji);
        ownReaction.setCreatedAt(Util.now());
    }

}
