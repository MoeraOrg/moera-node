package org.moera.node.data;

import java.sql.Timestamp;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.moera.node.auth.principal.Principal;
import org.moera.node.util.Util;

@Entity
@Table(name = "reactions")
public class Reaction {

    @Id
    private UUID id;

    @NotNull
    @Size(max = 63)
    private String ownerName = "";

    @Size(max = 96)
    private String ownerFullName;

    @Size(max = 31)
    private String ownerGender;

    @ManyToOne
    private MediaFile ownerAvatarMediaFile;

    @Size(max = 8)
    private String ownerAvatarShape;

    @ManyToOne
    @NotNull
    private EntryRevision entryRevision;

    @NotNull
    private boolean negative;

    @NotNull
    private int emoji;

    @NotNull
    private long moment;

    @NotNull
    private Timestamp createdAt = Util.now();

    private Timestamp deadline;

    private Timestamp deletedAt;

    private boolean replaced;

    private byte[] signature;

    private short signatureVersion;

    private Principal viewPrincipal = Principal.PUBLIC;

    private Principal postingViewPrincipal = Principal.UNSET;

    private Principal commentViewPrincipal = Principal.UNSET;

    private Principal postingDeletePrincipal = Principal.UNSET;

    private Principal commentDeletePrincipal = Principal.UNSET;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public MediaFile getOwnerAvatarMediaFile() {
        return ownerAvatarMediaFile;
    }

    public void setOwnerAvatarMediaFile(MediaFile ownerAvatarMediaFile) {
        this.ownerAvatarMediaFile = ownerAvatarMediaFile;
    }

    public String getOwnerAvatarShape() {
        return ownerAvatarShape;
    }

    public void setOwnerAvatarShape(String ownerAvatarShape) {
        this.ownerAvatarShape = ownerAvatarShape;
    }

    public EntryRevision getEntryRevision() {
        return entryRevision;
    }

    public void setEntryRevision(EntryRevision entryRevision) {
        this.entryRevision = entryRevision;
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

    public long getMoment() {
        return moment;
    }

    public void setMoment(long moment) {
        this.moment = moment;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getDeadline() {
        return deadline;
    }

    public void setDeadline(Timestamp deadline) {
        this.deadline = deadline;
    }

    public Timestamp getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Timestamp deletedAt) {
        this.deletedAt = deletedAt;
    }

    public boolean isReplaced() {
        return replaced;
    }

    public void setReplaced(boolean replaced) {
        this.replaced = replaced;
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

    private Principal toAbsolute(Principal principal) {
        Entry entry = getEntryRevision().getEntry();
        if (entry.getParent() == null) {
            return principal.withOwner(getOwnerName(), entry.getOwnerName());
        } else {
            return principal.withOwner(getOwnerName(), entry.getOwnerName(), entry.getParent().getOwnerName());
        }
    }

    public Principal getViewPrincipal() {
        return viewPrincipal;
    }

    public void setViewPrincipal(Principal viewPrincipal) {
        this.viewPrincipal = viewPrincipal;
    }

    public Principal getPostingViewPrincipal() {
        return postingViewPrincipal;
    }

    public void setPostingViewPrincipal(Principal postingViewPrincipal) {
        this.postingViewPrincipal = postingViewPrincipal;
    }

    public Principal getCommentViewPrincipal() {
        return commentViewPrincipal;
    }

    public void setCommentViewPrincipal(Principal commentViewPrincipal) {
        this.commentViewPrincipal = commentViewPrincipal;
    }

    public Principal getViewCompound() {
        return getPostingViewPrincipal().withSubordinate(getCommentViewPrincipal().withSubordinate(getViewPrincipal()));
    }

    public Principal getViewE() {
        return toAbsolute(getViewCompound());
    }

    public Principal getDeletePrincipal() {
        return Principal.PRIVATE;
    }

    public Principal getPostingDeletePrincipal() {
        return postingDeletePrincipal;
    }

    public void setPostingDeletePrincipal(Principal postingDeletePrincipal) {
        this.postingDeletePrincipal = postingDeletePrincipal;
    }

    public Principal getCommentDeletePrincipal() {
        return commentDeletePrincipal;
    }

    public void setCommentDeletePrincipal(Principal commentDeletePrincipal) {
        this.commentDeletePrincipal = commentDeletePrincipal;
    }

    public Principal getDeleteCompound() {
        return getPostingDeletePrincipal()
                .withSubordinate(getCommentDeletePrincipal().withSubordinate(getDeletePrincipal()));
    }

    public Principal getDeleteE() {
        return toAbsolute(getDeleteCompound());
    }

    public Principal getViewOperationsPrincipal() {
        return Principal.PRIVATE;
    }

    public Principal getViewOperationsE() {
        return toAbsolute(getViewOperationsPrincipal());
    }

}
