package org.moera.node.data;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.moera.commons.util.Util;

@Entity
@Table(name = "entries")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "entryType", discriminatorType = DiscriminatorType.INTEGER)
public class Entry {

    @Id
    private UUID id;

    @NotNull
    private UUID nodeId;

    @NotNull
    @Enumerated
    @Column(insertable = false, updatable = false)
    private EntryType entryType;

    @Size(max = 127)
    private String receiverName;

    @NotNull
    @Size(max = 127)
    private String ownerName = "";

    @NotNull
    private Timestamp createdAt = Util.now();

    private Timestamp deletedAt;

    private Timestamp deadline;

    @NotNull
    private boolean draft;

    @NotNull
    private int totalRevisions;

    @OneToOne
    private EntryRevision currentRevision;

    @OneToOne
    private EntryRevision draftRevision;

    @Size(max = 255)
    private String acceptedReactionsPositive;

    @Size(max = 255)
    private String acceptedReactionsNegative;

    @NotNull
    private boolean reactionsVisible = true;

    @NotNull
    private boolean reactionTotalsVisible = true;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "entry")
    private Set<EntryRevision> revisions = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "entry")
    private Set<ReactionTotal> reactionTotals = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "entry")
    private Set<Story> stories = new HashSet<>();

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

    public EntryType getEntryType() {
        return entryType;
    }

    protected void setEntryType(EntryType entryType) {
        this.entryType = entryType;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
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

    public Timestamp getDeadline() {
        return deadline;
    }

    public void setDeadline(Timestamp deadline) {
        this.deadline = deadline;
    }

    public boolean isDraft() {
        return draft;
    }

    public void setDraft(boolean draft) {
        this.draft = draft;
    }

    public int getTotalRevisions() {
        return totalRevisions;
    }

    public void setTotalRevisions(int totalRevisions) {
        this.totalRevisions = totalRevisions;
    }

    public EntryRevision getCurrentRevision() {
        return currentRevision;
    }

    public void setCurrentRevision(EntryRevision currentRevision) {
        this.currentRevision = currentRevision;
    }

    public EntryRevision getDraftRevision() {
        return draftRevision;
    }

    public void setDraftRevision(EntryRevision draftRevision) {
        this.draftRevision = draftRevision;
    }

    public String getAcceptedReactionsPositive() {
        return acceptedReactionsPositive;
    }

    public void setAcceptedReactionsPositive(String acceptedReactionsPositive) {
        this.acceptedReactionsPositive = acceptedReactionsPositive;
    }

    public String getAcceptedReactionsNegative() {
        return acceptedReactionsNegative;
    }

    public void setAcceptedReactionsNegative(String acceptedReactionsNegative) {
        this.acceptedReactionsNegative = acceptedReactionsNegative;
    }

    public boolean isReactionsVisible() {
        return reactionsVisible;
    }

    public void setReactionsVisible(boolean reactionsVisible) {
        this.reactionsVisible = reactionsVisible;
    }

    public boolean isReactionTotalsVisible() {
        return reactionTotalsVisible;
    }

    public void setReactionTotalsVisible(boolean reactionTotalsVisible) {
        this.reactionTotalsVisible = reactionTotalsVisible;
    }

    public Set<EntryRevision> getRevisions() {
        return revisions;
    }

    public void setRevisions(Set<EntryRevision> revisions) {
        this.revisions = revisions;
    }

    public void addRevision(EntryRevision revision) {
        revisions.add(revision);
        revision.setEntry(this);
    }

    public void removeRevision(EntryRevision revision) {
        revisions.removeIf(r -> r.getId().equals(revision.getId()));
        revision.setEntry(null);
    }

    public Set<ReactionTotal> getReactionTotals() {
        return reactionTotals;
    }

    public void setReactionTotals(Set<ReactionTotal> reactionTotals) {
        this.reactionTotals = reactionTotals;
    }

    public void addReactionTotal(ReactionTotal reactionTotal) {
        reactionTotals.add(reactionTotal);
        reactionTotal.setEntry(this);
    }

    public void removeReactionTotal(ReactionTotal reactionTotal) {
        reactionTotals.removeIf(rt -> rt.getId().equals(reactionTotal.getId()));
        reactionTotal.setEntry(null);
    }

    public Set<Story> getStories() {
        return stories;
    }

    public void setStories(Set<Story> stories) {
        this.stories = stories;
    }

    public void addStory(Story story) {
        stories.add(story);
        story.setEntry(this);
    }

    public void removeStory(Story story) {
        stories.removeIf(t -> t.getId().equals(story.getId()));
        story.setEntry(null);
    }

    public Story getStory(String feedName) {
        if (getStories() == null) {
            return null;
        }
        return getStories().stream().filter(fr -> fr.getFeedName().equals(feedName)).findFirst().orElse(null);
    }

}
