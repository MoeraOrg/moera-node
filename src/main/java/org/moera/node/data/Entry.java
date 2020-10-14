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
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
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

    @Size(max = 63)
    private String receiverName;

    @Size(max = 40)
    private String receiverEntryId;

    @NotNull
    @Size(max = 63)
    private String ownerName = "";

    @NotNull
    private Timestamp createdAt = Util.now();

    @NotNull
    private Timestamp editedAt = Util.now();

    private Timestamp deletedAt;

    private Timestamp receiverCreatedAt;

    private Timestamp receiverEditedAt;

    private Timestamp deadline;

    @NotNull
    private boolean draft;

    @NotNull
    private int totalRevisions;

    @OneToOne
    private EntryRevision currentRevision;

    @Size(max = 40)
    private String currentReceiverRevisionId;

    @OneToOne
    private EntryRevision draftRevision;

    @NotNull
    @Size(max = 255)
    private String acceptedReactionsPositive = "";

    @NotNull
    @Size(max = 255)
    private String acceptedReactionsNegative = "";

    @NotNull
    private boolean reactionsVisible = true;

    @NotNull
    private boolean reactionTotalsVisible = true;

    @ManyToOne
    private Entry parent;

    @NotNull
    private int totalChildren;

    private Long moment;

    @ManyToOne
    private Entry repliedTo;

    private String repliedToName;

    private String repliedToHeading;

    private byte[] repliedToDigest;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "entry")
    private Set<EntryRevision> revisions = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "entry")
    private Set<ReactionTotal> reactionTotals = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "entry")
    private Set<Story> stories = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "entry")
    private Set<EntrySource> sources = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "entry")
    private Set<Subscriber> subscribers = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "parent")
    private Set<Entry> children = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "repliedTo")
    private Set<Entry> replies = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "entry")
    private Set<PublicPage> publicPages = new HashSet<>();

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

    @Transient
    public boolean isOriginal() {
        return getReceiverName() == null;
    }

    public String getReceiverEntryId() {
        return receiverEntryId;
    }

    public void setReceiverEntryId(String receiverEntryId) {
        this.receiverEntryId = receiverEntryId;
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

    public Timestamp getEditedAt() {
        return editedAt;
    }

    public void setEditedAt(Timestamp editedAt) {
        this.editedAt = editedAt;
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

    public Timestamp getReceiverEditedAt() {
        return receiverEditedAt;
    }

    public void setReceiverEditedAt(Timestamp receiverEditedAt) {
        this.receiverEditedAt = receiverEditedAt;
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

    public boolean isMessage() {
        return getCurrentRevision() != null
                && BodyFormat.MESSAGE.getValue().equals(getCurrentRevision().getBodyFormat());
    }

    public String getCurrentReceiverRevisionId() {
        return currentReceiverRevisionId;
    }

    public void setCurrentReceiverRevisionId(String currentReceiverRevisionId) {
        this.currentReceiverRevisionId = currentReceiverRevisionId;
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

    public Entry getParent() {
        return parent;
    }

    public void setParent(Entry parent) {
        this.parent = parent;
    }

    public int getTotalChildren() {
        return totalChildren;
    }

    public void setTotalChildren(int totalChildren) {
        this.totalChildren = totalChildren;
    }

    public Long getMoment() {
        return moment;
    }

    public void setMoment(Long moment) {
        this.moment = moment;
    }

    public Entry getRepliedTo() {
        return repliedTo;
    }

    public void setRepliedTo(Entry repliedTo) {
        this.repliedTo = repliedTo;
    }

    public String getRepliedToName() {
        return repliedToName;
    }

    public void setRepliedToName(String repliedToName) {
        this.repliedToName = repliedToName;
    }

    public String getRepliedToHeading() {
        return repliedToHeading;
    }

    public void setRepliedToHeading(String repliedToHeading) {
        this.repliedToHeading = repliedToHeading;
    }

    public byte[] getRepliedToDigest() {
        return repliedToDigest;
    }

    public void setRepliedToDigest(byte[] repliedToDigest) {
        this.repliedToDigest = repliedToDigest;
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
        if (getStories() == null || feedName == null) {
            return null;
        }
        return getStories().stream().filter(fr -> feedName.equals(fr.getFeedName())).findFirst().orElse(null);
    }

    public Set<EntrySource> getSources() {
        return sources;
    }

    public void setSources(Set<EntrySource> sources) {
        this.sources = sources;
    }

    public void addSource(EntrySource source) {
        sources.add(source);
        source.setEntry(this);
    }

    public void removeSource(EntrySource source) {
        sources.removeIf(sr -> sr.getId().equals(source.getId()));
        source.setEntry(null);
    }

    public Set<Subscriber> getSubscribers() {
        return subscribers;
    }

    public void setSubscribers(Set<Subscriber> subscribers) {
        this.subscribers = subscribers;
    }

    public void addSubscriber(Subscriber subscriber) {
        subscribers.add(subscriber);
        subscriber.setEntry(this);
    }

    public void removeSubscriber(Subscriber subscriber) {
        subscribers.removeIf(sr -> sr.getId().equals(subscriber.getId()));
        subscriber.setEntry(null);
    }

    public Set<Entry> getChildren() {
        return children;
    }

    public void setChildren(Set<Entry> children) {
        this.children = children;
    }

    public void addChild(Entry child) {
        children.add(child);
        child.setParent(this);
    }

    public void removeChild(Entry child) {
        children.removeIf(sr -> sr.getId().equals(child.getId()));
        child.setParent(null);
    }

    public Set<Entry> getReplies() {
        return replies;
    }

    public void setReplies(Set<Entry> replies) {
        this.replies = replies;
    }

    public void addReply(Entry reply) {
        replies.add(reply);
        reply.setRepliedTo(this);
    }

    public void removeReply(Entry reply) {
        replies.removeIf(sr -> sr.getId().equals(reply.getId()));
        reply.setRepliedTo(null);
    }

    public Set<PublicPage> getPublicPages() {
        return publicPages;
    }

    public void setPublicPages(Set<PublicPage> publicPages) {
        this.publicPages = publicPages;
    }

    public void addPublicPage(PublicPage publicPage) {
        publicPages.add(publicPage);
        publicPage.setEntry(this);
    }

    public void removePublicPage(PublicPage publicPage) {
        publicPages.removeIf(pp -> pp.getId() == publicPage.getId());
        publicPage.setEntry(null);
    }

}
