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

import org.hibernate.annotations.TypeDef;
import org.moera.node.auth.principal.Principal;
import org.moera.node.auth.principal.PrincipalType;
import org.moera.node.util.Util;

@Entity
@Table(name = "entries")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "entryType", discriminatorType = DiscriminatorType.INTEGER)
@TypeDef(name = "Principal", typeClass = PrincipalType.class, defaultForType = Principal.class)
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

    @Size(max = 96)
    private String receiverFullName;

    @ManyToOne
    private MediaFile receiverAvatarMediaFile;

    @Size(max = 8)
    private String receiverAvatarShape;

    @Size(max = 40)
    private String receiverEntryId;

    @NotNull
    @Size(max = 63)
    private String ownerName = "";

    @Size(max = 96)
    private String ownerFullName;

    @ManyToOne
    private MediaFile ownerAvatarMediaFile;

    @Size(max = 8)
    private String ownerAvatarShape;

    @NotNull
    private Timestamp createdAt = Util.now();

    @NotNull
    private Timestamp editedAt = Util.now();

    private Timestamp deletedAt;

    private Timestamp receiverCreatedAt;

    private Timestamp receiverEditedAt;

    private Timestamp receiverDeletedAt;

    private Timestamp deadline;

    @NotNull
    private int totalRevisions;

    @OneToOne
    private EntryRevision currentRevision;

    @Size(max = 40)
    private String currentReceiverRevisionId;

    @NotNull
    @Size(max = 255)
    private String acceptedReactionsPositive = "";

    @NotNull
    @Size(max = 255)
    private String acceptedReactionsNegative = "";

    @ManyToOne
    private Entry parent;

    @ManyToOne
    private MediaFileOwner parentMedia;

    @NotNull
    private int totalChildren;

    private Long moment;

    @ManyToOne
    private Entry repliedTo;

    @ManyToOne
    private EntryRevision repliedToRevision;

    @Size(max = 63)
    private String repliedToName;

    @Size(max = 96)
    private String repliedToFullName;

    @ManyToOne
    private MediaFile repliedToAvatarMediaFile;

    @Size(max = 8)
    private String repliedToAvatarShape;

    @Size(max = 255)
    private String repliedToHeading;

    private byte[] repliedToDigest;

    private Principal viewPrincipal = Principal.PUBLIC;

    private Principal receiverViewPrincipal;

    private Principal receiverEditPrincipal;

    private Principal receiverDeletePrincipal;

    private Principal viewCommentsPrincipal = Principal.PUBLIC;

    private Principal receiverViewCommentsPrincipal;

    private Principal addCommentPrincipal = Principal.PUBLIC;

    private Principal receiverAddCommentPrincipal;

    private Principal viewReactionsPrincipal = Principal.PUBLIC;

    private Principal receiverViewReactionsPrincipal;

    private Principal viewNegativeReactionsPrincipal = Principal.PUBLIC;

    private Principal receiverViewNegativeReactionsPrincipal;

    private Principal viewReactionTotalsPrincipal = Principal.PUBLIC;

    private Principal receiverViewReactionTotalsPrincipal;

    private Principal viewNegativeReactionTotalsPrincipal = Principal.PUBLIC;

    private Principal receiverViewNegativeReactionTotalsPrincipal;

    private Principal viewReactionRatiosPrincipal = Principal.PUBLIC;

    private Principal receiverViewReactionRatiosPrincipal;

    private Principal viewNegativeReactionRatiosPrincipal = Principal.PUBLIC;

    private Principal receiverViewNegativeReactionRatiosPrincipal;

    private Principal addReactionPrincipal = Principal.PUBLIC;

    private Principal receiverAddReactionPrincipal;

    private Principal addNegativeReactionPrincipal = Principal.PUBLIC;

    private Principal receiverAddNegativeReactionPrincipal;

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

    @OneToOne(cascade = CascadeType.ALL, mappedBy = "entry")
    private SitemapRecord sitemapRecord;

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

    public String getReceiverFullName() {
        return receiverFullName;
    }

    public void setReceiverFullName(String receiverFullName) {
        this.receiverFullName = receiverFullName;
    }

    @Transient
    public boolean isOriginal() {
        return getReceiverName() == null;
    }

    public MediaFile getReceiverAvatarMediaFile() {
        return receiverAvatarMediaFile;
    }

    public void setReceiverAvatarMediaFile(MediaFile receiverAvatarMediaFile) {
        this.receiverAvatarMediaFile = receiverAvatarMediaFile;
    }

    public String getReceiverAvatarShape() {
        return receiverAvatarShape;
    }

    public void setReceiverAvatarShape(String receiverAvatarShape) {
        this.receiverAvatarShape = receiverAvatarShape;
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

    public String getOwnerFullName() {
        return ownerFullName;
    }

    public void setOwnerFullName(String ownerFullName) {
        this.ownerFullName = ownerFullName;
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

    public Timestamp getReceiverDeletedAt() {
        return receiverDeletedAt;
    }

    public void setReceiverDeletedAt(Timestamp receiverDeletedAt) {
        this.receiverDeletedAt = receiverDeletedAt;
    }

    public Timestamp getDeadline() {
        return deadline;
    }

    public void setDeadline(Timestamp deadline) {
        this.deadline = deadline;
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

    public MediaFileOwner getParentMedia() {
        return parentMedia;
    }

    public void setParentMedia(MediaFileOwner parentMedia) {
        this.parentMedia = parentMedia;
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

    public EntryRevision getRepliedToRevision() {
        return repliedToRevision;
    }

    public void setRepliedToRevision(EntryRevision repliedToRevision) {
        this.repliedToRevision = repliedToRevision;
    }

    public String getRepliedToName() {
        return repliedToName;
    }

    public void setRepliedToName(String repliedToName) {
        this.repliedToName = repliedToName;
    }

    public String getRepliedToFullName() {
        return repliedToFullName;
    }

    public void setRepliedToFullName(String repliedToFullName) {
        this.repliedToFullName = repliedToFullName;
    }

    public MediaFile getRepliedToAvatarMediaFile() {
        return repliedToAvatarMediaFile;
    }

    public void setRepliedToAvatarMediaFile(MediaFile repliedToAvatarMediaFile) {
        this.repliedToAvatarMediaFile = repliedToAvatarMediaFile;
    }

    public String getRepliedToAvatarShape() {
        return repliedToAvatarShape;
    }

    public void setRepliedToAvatarShape(String repliedToAvatarShape) {
        this.repliedToAvatarShape = repliedToAvatarShape;
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

    private Principal toAbsolute(Principal principal) {
        String parentOwnerName = getParent() != null ? getParent().getOwnerName() : null;
        return principal.withOwner(getOwnerName(), parentOwnerName);
    }

    private Principal toReceiverAbsolute(Principal principal) {
        return principal != null && getReceiverName() != null
                ? principal.withOwner(getReceiverName())
                : Principal.PUBLIC;
    }

    public Principal getViewPrincipal() {
        return viewPrincipal;
    }

    public Principal getViewE() {
        return toAbsolute(getViewPrincipal());
    }

    public void setViewPrincipal(Principal viewPrincipal) {
        this.viewPrincipal = viewPrincipal;
    }

    public Principal getReceiverViewPrincipal() {
        return receiverViewPrincipal;
    }

    public void setReceiverViewPrincipal(Principal receiverViewPrincipal) {
        this.receiverViewPrincipal = receiverViewPrincipal;
    }

    public Principal getEditPrincipal() {
        return receiverName == null ? Principal.OWNER : Principal.NONE;
    }

    public Principal getEditE() {
        return toAbsolute(getEditPrincipal());
    }

    public Principal getDeletePrincipal() {
        return receiverName == null ? Principal.PRIVATE : Principal.ADMIN;
    }

    public Principal getDeleteE() {
        return toAbsolute(getDeletePrincipal());
    }

    public Principal getReceiverEditPrincipal() {
        return receiverEditPrincipal;
    }

    public void setReceiverEditPrincipal(Principal receiverEditPrincipal) {
        this.receiverEditPrincipal = receiverEditPrincipal;
    }

    public Principal getReceiverDeletePrincipal() {
        return receiverDeletePrincipal;
    }

    public void setReceiverDeletePrincipal(Principal receiverDeletePrincipal) {
        this.receiverDeletePrincipal = receiverDeletePrincipal;
    }

    public Principal getViewCommentsPrincipal() {
        return viewCommentsPrincipal;
    }

    public Principal getViewCommentsE() {
        return toAbsolute(getViewCommentsPrincipal());
    }

    public void setViewCommentsPrincipal(Principal viewCommentsPrincipal) {
        this.viewCommentsPrincipal = viewCommentsPrincipal;
    }

    public Principal getReceiverViewCommentsPrincipal() {
        return receiverViewCommentsPrincipal;
    }

    public Principal getReceiverViewCommentsE() {
        return toReceiverAbsolute(getReceiverViewCommentsPrincipal());
    }

    public void setReceiverViewCommentsPrincipal(Principal receiverViewCommentsPrincipal) {
        this.receiverViewCommentsPrincipal = receiverViewCommentsPrincipal;
    }

    public Principal getAddCommentPrincipal() {
        return addCommentPrincipal;
    }

    public Principal getAddCommentE() {
        return toAbsolute(getAddCommentPrincipal());
    }

    public void setAddCommentPrincipal(Principal addCommentPrincipal) {
        this.addCommentPrincipal = addCommentPrincipal;
    }

    public Principal getReceiverAddCommentPrincipal() {
        return receiverAddCommentPrincipal;
    }

    public void setReceiverAddCommentPrincipal(Principal receiverAddCommentPrincipal) {
        this.receiverAddCommentPrincipal = receiverAddCommentPrincipal;
    }

    public Principal getViewReactionsPrincipal() {
        return viewReactionsPrincipal;
    }

    public Principal getViewReactionsE() {
        return toAbsolute(getViewReactionsPrincipal());
    }

    public void setViewReactionsPrincipal(Principal viewReactionsPrincipal) {
        this.viewReactionsPrincipal = viewReactionsPrincipal;
    }

    public Principal getReceiverViewReactionsPrincipal() {
        return receiverViewReactionsPrincipal;
    }

    public Principal getReceiverViewReactionsE() {
        return toReceiverAbsolute(getReceiverViewReactionsPrincipal());
    }

    public void setReceiverViewReactionsPrincipal(Principal receiverViewReactionsPrincipal) {
        this.receiverViewReactionsPrincipal = receiverViewReactionsPrincipal;
    }

    public Principal getViewNegativeReactionsPrincipal() {
        return viewNegativeReactionsPrincipal;
    }

    public Principal getViewNegativeReactionsE() {
        return toAbsolute(getViewNegativeReactionsPrincipal());
    }

    public void setViewNegativeReactionsPrincipal(Principal viewNegativeReactionsPrincipal) {
        this.viewNegativeReactionsPrincipal = viewNegativeReactionsPrincipal;
    }

    public Principal getReceiverViewNegativeReactionsPrincipal() {
        return receiverViewNegativeReactionsPrincipal;
    }

    public Principal getReceiverViewNegativeReactionsE() {
        return toReceiverAbsolute(getReceiverViewNegativeReactionsPrincipal());
    }

    public void setReceiverViewNegativeReactionsPrincipal(Principal receiverViewNegativeReactionsPrincipal) {
        this.receiverViewNegativeReactionsPrincipal = receiverViewNegativeReactionsPrincipal;
    }

    public Principal getViewReactionTotalsPrincipal() {
        return viewReactionTotalsPrincipal;
    }

    public Principal getViewReactionTotalsE() {
        return toAbsolute(getViewReactionTotalsPrincipal());
    }

    public void setViewReactionTotalsPrincipal(Principal viewReactionTotalsPrincipal) {
        this.viewReactionTotalsPrincipal = viewReactionTotalsPrincipal;
    }

    public Principal getReceiverViewReactionTotalsPrincipal() {
        return receiverViewReactionTotalsPrincipal;
    }

    public Principal getReceiverViewReactionTotalsE() {
        return toReceiverAbsolute(getReceiverViewReactionTotalsPrincipal());
    }

    public void setReceiverViewReactionTotalsPrincipal(Principal receiverViewReactionTotalsPrincipal) {
        this.receiverViewReactionTotalsPrincipal = receiverViewReactionTotalsPrincipal;
    }

    public Principal getViewNegativeReactionTotalsPrincipal() {
        return viewNegativeReactionTotalsPrincipal;
    }

    public Principal getViewNegativeReactionTotalsE() {
        return toAbsolute(getViewNegativeReactionTotalsPrincipal());
    }

    public void setViewNegativeReactionTotalsPrincipal(Principal viewNegativeReactionTotalsPrincipal) {
        this.viewNegativeReactionTotalsPrincipal = viewNegativeReactionTotalsPrincipal;
    }

    public Principal getReceiverViewNegativeReactionTotalsPrincipal() {
        return receiverViewNegativeReactionTotalsPrincipal;
    }

    public Principal getReceiverViewNegativeReactionTotalsE() {
        return toReceiverAbsolute(getReceiverViewNegativeReactionTotalsPrincipal());
    }

    public void setReceiverViewNegativeReactionTotalsPrincipal(Principal receiverViewNegativeReactionTotalsPrincipal) {
        this.receiverViewNegativeReactionTotalsPrincipal = receiverViewNegativeReactionTotalsPrincipal;
    }

    public Principal getViewReactionRatiosPrincipal() {
        return viewReactionRatiosPrincipal;
    }

    public Principal getViewReactionRatiosE() {
        return toAbsolute(getViewReactionRatiosPrincipal());
    }

    public void setViewReactionRatiosPrincipal(Principal viewReactionRatiosPrincipal) {
        this.viewReactionRatiosPrincipal = viewReactionRatiosPrincipal;
    }

    public Principal getReceiverViewReactionRatiosPrincipal() {
        return receiverViewReactionRatiosPrincipal;
    }

    public Principal getReceiverViewReactionRatiosE() {
        return toReceiverAbsolute(getReceiverViewReactionRatiosPrincipal());
    }

    public void setReceiverViewReactionRatiosPrincipal(Principal receiverViewReactionRatiosPrincipal) {
        this.receiverViewReactionRatiosPrincipal = receiverViewReactionRatiosPrincipal;
    }

    public Principal getViewNegativeReactionRatiosPrincipal() {
        return viewNegativeReactionRatiosPrincipal;
    }

    public Principal getViewNegativeReactionRatiosE() {
        return toAbsolute(getViewNegativeReactionRatiosPrincipal());
    }

    public void setViewNegativeReactionRatiosPrincipal(Principal viewNegativeReactionRatiosPrincipal) {
        this.viewNegativeReactionRatiosPrincipal = viewNegativeReactionRatiosPrincipal;
    }

    public Principal getReceiverViewNegativeReactionRatiosPrincipal() {
        return receiverViewNegativeReactionRatiosPrincipal;
    }

    public Principal getReceiverViewNegativeReactionRatiosE() {
        return toReceiverAbsolute(getReceiverViewNegativeReactionRatiosPrincipal());
    }

    public void setReceiverViewNegativeReactionRatiosPrincipal(Principal receiverViewNegativeReactionRatiosPrincipal) {
        this.receiverViewNegativeReactionRatiosPrincipal = receiverViewNegativeReactionRatiosPrincipal;
    }

    public Principal getAddReactionPrincipal() {
        return addReactionPrincipal;
    }

    public Principal getAddReactionE() {
        return toAbsolute(getAddReactionPrincipal());
    }

    public void setAddReactionPrincipal(Principal addReactionPrincipal) {
        this.addReactionPrincipal = addReactionPrincipal;
    }

    public Principal getReceiverAddReactionPrincipal() {
        return receiverAddReactionPrincipal;
    }

    public void setReceiverAddReactionPrincipal(Principal receiverAddReactionPrincipal) {
        this.receiverAddReactionPrincipal = receiverAddReactionPrincipal;
    }

    public Principal getAddNegativeReactionPrincipal() {
        return addNegativeReactionPrincipal;
    }

    public Principal getAddNegativeReactionE() {
        return toAbsolute(getAddNegativeReactionPrincipal());
    }

    public void setAddNegativeReactionPrincipal(Principal addNegativeReactionPrincipal) {
        this.addNegativeReactionPrincipal = addNegativeReactionPrincipal;
    }

    public Principal getReceiverAddNegativeReactionPrincipal() {
        return receiverAddNegativeReactionPrincipal;
    }

    public void setReceiverAddNegativeReactionPrincipal(Principal receiverAddNegativeReactionPrincipal) {
        this.receiverAddNegativeReactionPrincipal = receiverAddNegativeReactionPrincipal;
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

    public SitemapRecord getSitemapRecord() {
        return sitemapRecord;
    }

    public void setSitemapRecord(SitemapRecord sitemapRecord) {
        if (this.sitemapRecord != null) {
            this.sitemapRecord.setEntry(null);
        }
        this.sitemapRecord = sitemapRecord;
        if (sitemapRecord != null) {
            sitemapRecord.setEntry(this);
        }
    }

}
