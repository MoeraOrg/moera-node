package org.moera.node.data;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.moera.commons.util.Util;

@Entity
@Table(name = "entry_revisions")
public class EntryRevision {

    @Id
    private UUID id;

    @Size(max = 40)
    private String receiverRevisionId;

    @ManyToOne
    @NotNull
    private Entry entry;

    @NotNull
    private String bodyPreview = "";

    private String saneBodyPreview;

    @NotNull
    private String bodySrc = "";

    private byte[] receiverBodySrcHash;

    @NotNull
    @Enumerated
    private SourceFormat bodySrcFormat = SourceFormat.PLAIN_TEXT;

    @NotNull
    @Size(max = 75)
    private String bodyFormat = BodyFormat.MESSAGE.getValue();

    @NotNull
    private String body = "";

    private String saneBody;

    @NotNull
    private String heading = "";

    @NotNull
    private Timestamp createdAt = Util.now();

    private Timestamp deletedAt;

    private Timestamp receiverCreatedAt;

    private Timestamp receiverDeletedAt;

    private byte[] signature;

    @NotNull
    private short signatureVersion;

    private byte[] digest;

    @ManyToOne
    private EntryRevision parent;

    private Timestamp deadline;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "entryRevision")
    private Set<Reaction> reactions = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "entryRevision")
    private Set<ReactionTotal> reactionTotals = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "parent")
    private Set<EntryRevision> children = new HashSet<>();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getReceiverRevisionId() {
        return receiverRevisionId;
    }

    public void setReceiverRevisionId(String receiverRevisionId) {
        this.receiverRevisionId = receiverRevisionId;
    }

    public Entry getEntry() {
        return entry;
    }

    public void setEntry(Entry entry) {
        this.entry = entry;
    }

    public String getBodyPreview() {
        return bodyPreview;
    }

    public void setBodyPreview(String bodyPreview) {
        this.bodyPreview = bodyPreview;
    }

    public String getSaneBodyPreview() {
        return saneBodyPreview;
    }

    public void setSaneBodyPreview(String saneBodyPreview) {
        this.saneBodyPreview = saneBodyPreview;
    }

    public String getBodySrc() {
        return bodySrc;
    }

    public void setBodySrc(String bodySrc) {
        this.bodySrc = bodySrc;
    }

    public byte[] getReceiverBodySrcHash() {
        return receiverBodySrcHash;
    }

    public void setReceiverBodySrcHash(byte[] receiverBodySrcHash) {
        this.receiverBodySrcHash = receiverBodySrcHash;
    }

    public SourceFormat getBodySrcFormat() {
        return bodySrcFormat;
    }

    public void setBodySrcFormat(SourceFormat bodySrcFormat) {
        this.bodySrcFormat = bodySrcFormat;
    }

    public String getBodyFormat() {
        return bodyFormat;
    }

    public void setBodyFormat(String bodyFormat) {
        this.bodyFormat = bodyFormat;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getSaneBody() {
        return saneBody;
    }

    public void setSaneBody(String saneBody) {
        this.saneBody = saneBody;
    }

    public String getHeading() {
        return heading;
    }

    public void setHeading(String heading) {
        this.heading = heading;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Timestamp deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Timestamp getReceiverCreatedAt() {
        return receiverCreatedAt;
    }

    public void setReceiverCreatedAt(Timestamp receiverCreatedAt) {
        this.receiverCreatedAt = receiverCreatedAt;
    }

    public Timestamp getReceiverDeletedAt() {
        return receiverDeletedAt;
    }

    public void setReceiverDeletedAt(Timestamp receiverDeletedAt) {
        this.receiverDeletedAt = receiverDeletedAt;
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

    public byte[] getDigest() {
        return digest;
    }

    public void setDigest(byte[] digest) {
        this.digest = digest;
    }

    public EntryRevision getParent() {
        return parent;
    }

    public void setParent(EntryRevision parent) {
        this.parent = parent;
    }

    public Timestamp getDeadline() {
        return deadline;
    }

    public void setDeadline(Timestamp deadline) {
        this.deadline = deadline;
    }

    public Set<Reaction> getReactions() {
        return reactions;
    }

    public void setReactions(Set<Reaction> reactions) {
        this.reactions = reactions;
    }

    public void addReaction(Reaction reaction) {
        reactions.add(reaction);
        reaction.setEntryRevision(this);
    }

    public void removeReaction(Reaction reaction) {
        reactions.removeIf(r -> r.getId().equals(reaction.getId()));
        reaction.setEntryRevision(null);
    }

    public Set<ReactionTotal> getReactionTotals() {
        return reactionTotals;
    }

    public void setReactionTotals(Set<ReactionTotal> reactionTotals) {
        this.reactionTotals = reactionTotals;
    }

    public void addReactionTotal(ReactionTotal reactionTotal) {
        reactionTotals.add(reactionTotal);
        reactionTotal.setEntryRevision(this);
    }

    public void removeReactionTotal(ReactionTotal reactionTotal) {
        reactionTotals.removeIf(rt -> rt.getId().equals(reactionTotal.getId()));
        reactionTotal.setEntryRevision(null);
    }

    public Set<EntryRevision> getChildren() {
        return children;
    }

    public void setChildren(Set<EntryRevision> children) {
        this.children = children;
    }

    public void addChild(EntryRevision child) {
        children.add(child);
        child.setParent(this);
    }

    public void removeChild(EntryRevision child) {
        children.removeIf(sr -> sr.getId().equals(child.getId()));
        child.setParent(null);
    }

}
