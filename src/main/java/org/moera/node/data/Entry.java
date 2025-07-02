package org.moera.node.data;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.moera.lib.node.types.BodyFormat;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.util.Util;

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

    @Size(max = 96)
    private String receiverFullName;

    @Size(max = 31)
    private String receiverGender;

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

    @Size(max = 31)
    private String ownerGender;

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
    private String rejectedReactionsPositive = "";

    @NotNull
    @Size(max = 255)
    private String rejectedReactionsNegative = "*";

    @NotNull
    @Size(max = 255)
    private String parentRejectedReactionsPositive = "";

    @NotNull
    @Size(max = 255)
    private String parentRejectedReactionsNegative = "";

    @NotNull
    @Size(max = 255)
    private String childRejectedReactionsPositive = "";

    @NotNull
    @Size(max = 255)
    private String childRejectedReactionsNegative = "";

    @ManyToOne(fetch = FetchType.LAZY)
    private Entry parent;

    @ManyToOne(fetch = FetchType.LAZY)
    private MediaFileOwner parentMedia;

    @NotNull
    private int totalChildren;

    private Long moment;

    @ManyToOne(fetch = FetchType.LAZY)
    private Entry repliedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    private EntryRevision repliedToRevision;

    @Size(max = 63)
    private String repliedToName;

    @Size(max = 96)
    private String repliedToFullName;

    @Size(max = 31)
    private String repliedToGender;

    @ManyToOne(fetch = FetchType.LAZY)
    private MediaFile repliedToAvatarMediaFile;

    @Size(max = 8)
    private String repliedToAvatarShape;

    @Size(max = 255)
    private String repliedToHeading;

    private byte[] repliedToDigest;

    private Principal viewPrincipal = Principal.PUBLIC;

    private Principal parentViewPrincipal = Principal.UNSET;

    private Principal receiverViewPrincipal;

    private Principal parentEditPrincipal = Principal.UNSET;

    private Principal receiverEditPrincipal;

    private Principal parentDeletePrincipal = Principal.UNSET;

    private Principal receiverDeletePrincipal;

    private Principal viewCommentsPrincipal = Principal.PUBLIC;

    private Principal parentViewCommentsPrincipal = Principal.UNSET;

    private Principal receiverViewCommentsPrincipal;

    private Principal addCommentPrincipal = Principal.SIGNED;

    private Principal parentAddCommentPrincipal = Principal.UNSET;

    private Principal receiverAddCommentPrincipal;

    private Principal parentOverrideCommentPrincipal = Principal.UNSET;

    private Principal receiverOverrideCommentPrincipal;

    private Principal viewReactionsPrincipal = Principal.PUBLIC;

    private Principal parentViewReactionsPrincipal = Principal.UNSET;

    private Principal receiverViewReactionsPrincipal;

    private Principal viewNegativeReactionsPrincipal = Principal.PUBLIC;

    private Principal parentViewNegativeReactionsPrincipal = Principal.UNSET;

    private Principal receiverViewNegativeReactionsPrincipal;

    private Principal viewReactionTotalsPrincipal = Principal.PUBLIC;

    private Principal parentViewReactionTotalsPrincipal = Principal.UNSET;

    private Principal receiverViewReactionTotalsPrincipal;

    private Principal viewNegativeReactionTotalsPrincipal = Principal.PUBLIC;

    private Principal parentViewNegativeReactionTotalsPrincipal = Principal.UNSET;

    private Principal receiverViewNegativeReactionTotalsPrincipal;

    private Principal viewReactionRatiosPrincipal = Principal.PUBLIC;

    private Principal parentViewReactionRatiosPrincipal = Principal.UNSET;

    private Principal receiverViewReactionRatiosPrincipal;

    private Principal viewNegativeReactionRatiosPrincipal = Principal.PUBLIC;

    private Principal parentViewNegativeReactionRatiosPrincipal = Principal.UNSET;

    private Principal receiverViewNegativeReactionRatiosPrincipal;

    private Principal addReactionPrincipal = Principal.SIGNED;

    private Principal parentAddReactionPrincipal = Principal.UNSET;

    private Principal receiverAddReactionPrincipal;

    private Principal addNegativeReactionPrincipal = Principal.SIGNED;

    private Principal parentAddNegativeReactionPrincipal = Principal.UNSET;

    private Principal receiverAddNegativeReactionPrincipal;

    private Principal parentOverrideReactionPrincipal = Principal.UNSET;

    private Principal receiverOverrideReactionPrincipal;

    private Principal parentOverrideCommentReactionPrincipal = Principal.UNSET;

    private Principal receiverOverrideCommentReactionPrincipal;

    private ChildOperations childOperations = new ChildOperations();

    private ChildOperations reactionOperations = new ChildOperations();

    private ChildOperations childReactionOperations = new ChildOperations();

    private String receiverSheriffs;

    private String sheriffMarks = "";

    private String receiverSheriffMarks;

    private boolean sheriffUserListReferred;

    private boolean recommended;

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

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "entry")
    private Set<BlockedInstant> blockedInstants = new HashSet<>();

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

    public String getReceiverFullName() {
        return receiverFullName;
    }

    public void setReceiverFullName(String receiverFullName) {
        this.receiverFullName = receiverFullName;
    }

    public String getReceiverGender() {
        return receiverGender;
    }

    public void setReceiverGender(String receiverGender) {
        this.receiverGender = receiverGender;
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

    public String getRejectedReactionsPositive() {
        return rejectedReactionsPositive;
    }

    public void setRejectedReactionsPositive(String rejectedReactionsPositive) {
        this.rejectedReactionsPositive = rejectedReactionsPositive;
    }

    public String getRejectedReactionsNegative() {
        return rejectedReactionsNegative;
    }

    public void setRejectedReactionsNegative(String rejectedReactionsNegative) {
        this.rejectedReactionsNegative = rejectedReactionsNegative;
    }

    public String getParentRejectedReactionsPositive() {
        return parentRejectedReactionsPositive;
    }

    public void setParentRejectedReactionsPositive(String parentRejectedReactionsPositive) {
        this.parentRejectedReactionsPositive = parentRejectedReactionsPositive;
    }

    public String getParentRejectedReactionsNegative() {
        return parentRejectedReactionsNegative;
    }

    public void setParentRejectedReactionsNegative(String parentRejectedReactionsNegative) {
        this.parentRejectedReactionsNegative = parentRejectedReactionsNegative;
    }

    public String getChildRejectedReactionsPositive() {
        return childRejectedReactionsPositive;
    }

    public void setChildRejectedReactionsPositive(String childRejectedReactionsPositive) {
        this.childRejectedReactionsPositive = childRejectedReactionsPositive;
    }

    public String getChildRejectedReactionsNegative() {
        return childRejectedReactionsNegative;
    }

    public void setChildRejectedReactionsNegative(String childRejectedReactionsNegative) {
        this.childRejectedReactionsNegative = childRejectedReactionsNegative;
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

    public String getRepliedToGender() {
        return repliedToGender;
    }

    public void setRepliedToGender(String repliedToGender) {
        this.repliedToGender = repliedToGender;
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
        if (getParent() == null) {
            return principal.withOwner(getOwnerName());
        } else {
            return principal.withOwner(getOwnerName(), getParent().getOwnerName());
        }
    }

    private Principal toReceiverAbsolute(Principal principal) {
        return principal != null && getReceiverName() != null
                ? principal.withOwner(getReceiverName())
                : Principal.PUBLIC;
    }

    public Principal getViewPrincipal() {
        return viewPrincipal;
    }

    public void setViewPrincipal(Principal viewPrincipal) {
        this.viewPrincipal = viewPrincipal;
    }

    public Principal getParentViewPrincipal() {
        return parentViewPrincipal;
    }

    public void setParentViewPrincipal(Principal parentViewPrincipal) {
        this.parentViewPrincipal = parentViewPrincipal;
    }

    public Principal getViewCompound() {
        return getParentViewPrincipal().withSubordinate(getViewPrincipal());
    }

    public Principal getViewE() {
        return toAbsolute(getViewCompound());
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

    public Principal getParentEditPrincipal() {
        return parentEditPrincipal;
    }

    public void setParentEditPrincipal(Principal parentEditPrincipal) {
        this.parentEditPrincipal = parentEditPrincipal;
    }

    public Principal getEditCompound() {
        return getParentEditPrincipal().withSubordinate(getEditPrincipal());
    }

    public Principal getEditE() {
        return toAbsolute(getEditCompound());
    }

    public Principal getReceiverEditPrincipal() {
        return receiverEditPrincipal;
    }

    public void setReceiverEditPrincipal(Principal receiverEditPrincipal) {
        this.receiverEditPrincipal = receiverEditPrincipal;
    }

    public Principal getDeletePrincipal() {
        return receiverName == null ? Principal.PRIVATE : Principal.ADMIN;
    }

    public Principal getParentDeletePrincipal() {
        return parentDeletePrincipal;
    }

    public void setParentDeletePrincipal(Principal parentDeletePrincipal) {
        this.parentDeletePrincipal = parentDeletePrincipal;
    }

    public Principal getDeleteCompound() {
        return getParentDeletePrincipal().withSubordinate(getDeletePrincipal());
    }

    public Principal getDeleteE() {
        return toAbsolute(getDeleteCompound());
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

    public void setViewCommentsPrincipal(Principal viewCommentsPrincipal) {
        this.viewCommentsPrincipal = viewCommentsPrincipal;
    }

    public Principal getParentViewCommentsPrincipal() {
        return parentViewCommentsPrincipal;
    }

    public void setParentViewCommentsPrincipal(Principal parentViewCommentsPrincipal) {
        this.parentViewCommentsPrincipal = parentViewCommentsPrincipal;
    }

    public Principal getViewCommentsCompound() {
        return getParentViewCommentsPrincipal().withSubordinate(getViewCommentsPrincipal());
    }

    public Principal getViewCommentsE() {
        return toAbsolute(getViewCommentsCompound());
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

    public void setAddCommentPrincipal(Principal addCommentPrincipal) {
        this.addCommentPrincipal = addCommentPrincipal;
    }

    public Principal getParentAddCommentPrincipal() {
        return parentAddCommentPrincipal;
    }

    public void setParentAddCommentPrincipal(Principal parentAddCommentPrincipal) {
        this.parentAddCommentPrincipal = parentAddCommentPrincipal;
    }

    public Principal getAddCommentCompound() {
        return getParentAddCommentPrincipal().withSubordinate(getAddCommentPrincipal());
    }

    public Principal getAddCommentE() {
        return toAbsolute(getAddCommentCompound());
    }

    public Principal getReceiverAddCommentPrincipal() {
        return receiverAddCommentPrincipal;
    }

    public void setReceiverAddCommentPrincipal(Principal receiverAddCommentPrincipal) {
        this.receiverAddCommentPrincipal = receiverAddCommentPrincipal;
    }

    public Principal getOverrideCommentPrincipal() {
        return receiverName == null ? Principal.OWNER : Principal.NONE;
    }

    public Principal getParentOverrideCommentPrincipal() {
        return parentOverrideCommentPrincipal;
    }

    public void setParentOverrideCommentPrincipal(Principal parentOverrideCommentPrincipal) {
        this.parentOverrideCommentPrincipal = parentOverrideCommentPrincipal;
    }

    public Principal getOverrideCommentCompound() {
        return getParentOverrideCommentPrincipal().withSubordinate(getOverrideCommentPrincipal());
    }

    public Principal getOverrideCommentE() {
        return toAbsolute(getOverrideCommentCompound());
    }

    public Principal getReceiverOverrideCommentPrincipal() {
        return receiverOverrideCommentPrincipal;
    }

    public void setReceiverOverrideCommentPrincipal(Principal receiverOverrideCommentPrincipal) {
        this.receiverOverrideCommentPrincipal = receiverOverrideCommentPrincipal;
    }

    public Principal getViewReactionsPrincipal() {
        return viewReactionsPrincipal;
    }

    public void setViewReactionsPrincipal(Principal viewReactionsPrincipal) {
        this.viewReactionsPrincipal = viewReactionsPrincipal;
    }

    public Principal getParentViewReactionsPrincipal() {
        return parentViewReactionsPrincipal;
    }

    public void setParentViewReactionsPrincipal(Principal parentViewReactionsPrincipal) {
        this.parentViewReactionsPrincipal = parentViewReactionsPrincipal;
    }

    public Principal getViewReactionsCompound() {
        return getParentViewReactionsPrincipal().withSubordinate(getViewReactionsPrincipal());
    }

    public Principal getViewReactionsE() {
        return toAbsolute(getViewReactionsCompound());
    }

    public Principal getReceiverViewReactionsPrincipal() {
        return receiverViewReactionsPrincipal;
    }

    public void setReceiverViewReactionsPrincipal(Principal receiverViewReactionsPrincipal) {
        this.receiverViewReactionsPrincipal = receiverViewReactionsPrincipal;
    }

    public Principal getReceiverViewReactionsE() {
        return toReceiverAbsolute(getReceiverViewReactionsPrincipal());
    }

    public Principal getViewNegativeReactionsPrincipal() {
        return viewNegativeReactionsPrincipal;
    }

    public void setViewNegativeReactionsPrincipal(Principal viewNegativeReactionsPrincipal) {
        this.viewNegativeReactionsPrincipal = viewNegativeReactionsPrincipal;
    }

    public Principal getParentViewNegativeReactionsPrincipal() {
        return parentViewNegativeReactionsPrincipal;
    }

    public void setParentViewNegativeReactionsPrincipal(Principal parentViewNegativeReactionsPrincipal) {
        this.parentViewNegativeReactionsPrincipal = parentViewNegativeReactionsPrincipal;
    }

    public Principal getViewNegativeReactionsCompound() {
        return getParentViewNegativeReactionsPrincipal().withSubordinate(getViewNegativeReactionsPrincipal());
    }

    public Principal getViewNegativeReactionsE() {
        return toAbsolute(getViewNegativeReactionsCompound());
    }

    public Principal getReceiverViewNegativeReactionsPrincipal() {
        return receiverViewNegativeReactionsPrincipal;
    }

    public void setReceiverViewNegativeReactionsPrincipal(Principal receiverViewNegativeReactionsPrincipal) {
        this.receiverViewNegativeReactionsPrincipal = receiverViewNegativeReactionsPrincipal;
    }

    public Principal getReceiverViewNegativeReactionsE() {
        return toReceiverAbsolute(getReceiverViewNegativeReactionsPrincipal());
    }

    public Principal getViewReactionTotalsPrincipal() {
        return viewReactionTotalsPrincipal;
    }

    public void setViewReactionTotalsPrincipal(Principal viewReactionTotalsPrincipal) {
        this.viewReactionTotalsPrincipal = viewReactionTotalsPrincipal;
    }

    public Principal getParentViewReactionTotalsPrincipal() {
        return parentViewReactionTotalsPrincipal;
    }

    public void setParentViewReactionTotalsPrincipal(Principal parentViewReactionTotalsPrincipal) {
        this.parentViewReactionTotalsPrincipal = parentViewReactionTotalsPrincipal;
    }

    public Principal getViewReactionTotalsCompound() {
        return getParentViewReactionTotalsPrincipal().withSubordinate(getViewReactionTotalsPrincipal());
    }

    public Principal getViewReactionTotalsE() {
        return toAbsolute(getViewReactionTotalsCompound());
    }

    public Principal getReceiverViewReactionTotalsPrincipal() {
        return receiverViewReactionTotalsPrincipal;
    }

    public void setReceiverViewReactionTotalsPrincipal(Principal receiverViewReactionTotalsPrincipal) {
        this.receiverViewReactionTotalsPrincipal = receiverViewReactionTotalsPrincipal;
    }

    public Principal getReceiverViewReactionTotalsE() {
        return toReceiverAbsolute(getReceiverViewReactionTotalsPrincipal());
    }

    public Principal getViewNegativeReactionTotalsPrincipal() {
        return viewNegativeReactionTotalsPrincipal;
    }

    public void setViewNegativeReactionTotalsPrincipal(Principal viewNegativeReactionTotalsPrincipal) {
        this.viewNegativeReactionTotalsPrincipal = viewNegativeReactionTotalsPrincipal;
    }

    public Principal getParentViewNegativeReactionTotalsPrincipal() {
        return parentViewNegativeReactionTotalsPrincipal;
    }

    public void setParentViewNegativeReactionTotalsPrincipal(Principal parentViewNegativeReactionTotalsPrincipal) {
        this.parentViewNegativeReactionTotalsPrincipal = parentViewNegativeReactionTotalsPrincipal;
    }

    public Principal getViewNegativeReactionTotalsCompound() {
        return getParentViewNegativeReactionTotalsPrincipal().withSubordinate(getViewNegativeReactionTotalsPrincipal());
    }

    public Principal getViewNegativeReactionTotalsE() {
        return toAbsolute(getViewNegativeReactionTotalsCompound());
    }

    public Principal getReceiverViewNegativeReactionTotalsPrincipal() {
        return receiverViewNegativeReactionTotalsPrincipal;
    }

    public void setReceiverViewNegativeReactionTotalsPrincipal(Principal receiverViewNegativeReactionTotalsPrincipal) {
        this.receiverViewNegativeReactionTotalsPrincipal = receiverViewNegativeReactionTotalsPrincipal;
    }

    public Principal getReceiverViewNegativeReactionTotalsE() {
        return toReceiverAbsolute(getReceiverViewNegativeReactionTotalsPrincipal());
    }

    public Principal getViewReactionRatiosPrincipal() {
        return viewReactionRatiosPrincipal;
    }

    public void setViewReactionRatiosPrincipal(Principal viewReactionRatiosPrincipal) {
        this.viewReactionRatiosPrincipal = viewReactionRatiosPrincipal;
    }

    public Principal getParentViewReactionRatiosPrincipal() {
        return parentViewReactionRatiosPrincipal;
    }

    public void setParentViewReactionRatiosPrincipal(Principal parentViewReactionRatiosPrincipal) {
        this.parentViewReactionRatiosPrincipal = parentViewReactionRatiosPrincipal;
    }

    public Principal getViewReactionRatiosCompound() {
        return getParentViewReactionRatiosPrincipal().withSubordinate(getViewReactionRatiosPrincipal());
    }

    public Principal getViewReactionRatiosE() {
        return toAbsolute(getViewReactionRatiosCompound());
    }

    public Principal getReceiverViewReactionRatiosPrincipal() {
        return receiverViewReactionRatiosPrincipal;
    }

    public void setReceiverViewReactionRatiosPrincipal(Principal receiverViewReactionRatiosPrincipal) {
        this.receiverViewReactionRatiosPrincipal = receiverViewReactionRatiosPrincipal;
    }

    public Principal getReceiverViewReactionRatiosE() {
        return toReceiverAbsolute(getReceiverViewReactionRatiosPrincipal());
    }

    public Principal getViewNegativeReactionRatiosPrincipal() {
        return viewNegativeReactionRatiosPrincipal;
    }

    public void setViewNegativeReactionRatiosPrincipal(Principal viewNegativeReactionRatiosPrincipal) {
        this.viewNegativeReactionRatiosPrincipal = viewNegativeReactionRatiosPrincipal;
    }

    public Principal getParentViewNegativeReactionRatiosPrincipal() {
        return parentViewNegativeReactionRatiosPrincipal;
    }

    public void setParentViewNegativeReactionRatiosPrincipal(Principal parentViewNegativeReactionRatiosPrincipal) {
        this.parentViewNegativeReactionRatiosPrincipal = parentViewNegativeReactionRatiosPrincipal;
    }

    public Principal getViewNegativeReactionRatiosCompound() {
        return getParentViewNegativeReactionRatiosPrincipal().withSubordinate(getViewNegativeReactionRatiosPrincipal());
    }

    public Principal getViewNegativeReactionRatiosE() {
        return toAbsolute(getViewNegativeReactionRatiosCompound());
    }

    public Principal getReceiverViewNegativeReactionRatiosPrincipal() {
        return receiverViewNegativeReactionRatiosPrincipal;
    }

    public void setReceiverViewNegativeReactionRatiosPrincipal(Principal receiverViewNegativeReactionRatiosPrincipal) {
        this.receiverViewNegativeReactionRatiosPrincipal = receiverViewNegativeReactionRatiosPrincipal;
    }

    public Principal getReceiverViewNegativeReactionRatiosE() {
        return toReceiverAbsolute(getReceiverViewNegativeReactionRatiosPrincipal());
    }

    public Principal getAddReactionPrincipal() {
        return addReactionPrincipal;
    }

    public void setAddReactionPrincipal(Principal addReactionPrincipal) {
        this.addReactionPrincipal = addReactionPrincipal;
    }

    public Principal getParentAddReactionPrincipal() {
        return parentAddReactionPrincipal;
    }

    public void setParentAddReactionPrincipal(Principal parentAddReactionPrincipal) {
        this.parentAddReactionPrincipal = parentAddReactionPrincipal;
    }

    public Principal getAddReactionCompound() {
        return getParentAddReactionPrincipal().withSubordinate(getAddReactionPrincipal());
    }

    public Principal getAddReactionE() {
        return toAbsolute(getAddReactionCompound());
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

    public void setAddNegativeReactionPrincipal(Principal addNegativeReactionPrincipal) {
        this.addNegativeReactionPrincipal = addNegativeReactionPrincipal;
    }

    public Principal getParentAddNegativeReactionPrincipal() {
        return parentAddNegativeReactionPrincipal;
    }

    public void setParentAddNegativeReactionPrincipal(Principal parentAddNegativeReactionPrincipal) {
        this.parentAddNegativeReactionPrincipal = parentAddNegativeReactionPrincipal;
    }

    public Principal getAddNegativeReactionCompound() {
        return getParentAddNegativeReactionPrincipal().withSubordinate(getAddNegativeReactionPrincipal());
    }

    public Principal getAddNegativeReactionE() {
        return toAbsolute(getAddNegativeReactionCompound());
    }

    public Principal getReceiverAddNegativeReactionPrincipal() {
        return receiverAddNegativeReactionPrincipal;
    }

    public void setReceiverAddNegativeReactionPrincipal(Principal receiverAddNegativeReactionPrincipal) {
        this.receiverAddNegativeReactionPrincipal = receiverAddNegativeReactionPrincipal;
    }

    public Principal getOverrideReactionPrincipal() {
        return receiverName == null ? Principal.OWNER : Principal.NONE;
    }

    public Principal getParentOverrideReactionPrincipal() {
        return parentOverrideReactionPrincipal;
    }

    public void setParentOverrideReactionPrincipal(Principal parentOverrideReactionPrincipal) {
        this.parentOverrideReactionPrincipal = parentOverrideReactionPrincipal;
    }

    public Principal getOverrideReactionCompound() {
        return getParentOverrideReactionPrincipal().withSubordinate(getOverrideReactionPrincipal());
    }

    public Principal getOverrideReactionE() {
        return toAbsolute(getOverrideReactionCompound());
    }

    public Principal getReceiverOverrideReactionPrincipal() {
        return receiverOverrideReactionPrincipal;
    }

    public void setReceiverOverrideReactionPrincipal(Principal receiverOverrideReactionPrincipal) {
        this.receiverOverrideReactionPrincipal = receiverOverrideReactionPrincipal;
    }

    public Principal getOverrideCommentReactionPrincipal() {
        return receiverName == null ? Principal.OWNER : Principal.NONE;
    }

    public Principal getParentOverrideCommentReactionPrincipal() {
        return parentOverrideCommentReactionPrincipal;
    }

    public void setParentOverrideCommentReactionPrincipal(Principal parentOverrideCommentReactionPrincipal) {
        this.parentOverrideCommentReactionPrincipal = parentOverrideCommentReactionPrincipal;
    }

    public Principal getOverrideCommentReactionCompound() {
        return getParentOverrideCommentReactionPrincipal().withSubordinate(getOverrideCommentReactionPrincipal());
    }

    public Principal getOverrideCommentReactionE() {
        return toAbsolute(getOverrideCommentReactionCompound());
    }

    public Principal getReceiverOverrideCommentReactionPrincipal() {
        return receiverOverrideCommentReactionPrincipal;
    }

    public void setReceiverOverrideCommentReactionPrincipal(Principal receiverOverrideCommentReactionPrincipal) {
        this.receiverOverrideCommentReactionPrincipal = receiverOverrideCommentReactionPrincipal;
    }

    public Principal getViewOperationsPrincipal() {
        return Principal.PRIVATE;
    }

    public Principal getViewOperationsE() {
        return toAbsolute(getViewOperationsPrincipal());
    }

    public ChildOperations getChildOperations() {
        return childOperations;
    }

    public void setChildOperations(ChildOperations childOperations) {
        this.childOperations = childOperations;
    }

    public ChildOperations getReactionOperations() {
        return reactionOperations;
    }

    public void setReactionOperations(ChildOperations reactionOperations) {
        this.reactionOperations = reactionOperations;
    }

    public ChildOperations getChildReactionOperations() {
        return childReactionOperations;
    }

    public void setChildReactionOperations(ChildOperations childReactionOperations) {
        this.childReactionOperations = childReactionOperations;
    }

    public String getReceiverSheriffs() {
        return receiverSheriffs;
    }

    public void setReceiverSheriffs(String receiverSheriffs) {
        this.receiverSheriffs = receiverSheriffs;
    }

    public String getSheriffMarks() {
        return sheriffMarks;
    }

    public void setSheriffMarks(String sheriffMarks) {
        this.sheriffMarks = sheriffMarks;
    }

    public String getReceiverSheriffMarks() {
        return receiverSheriffMarks;
    }

    public void setReceiverSheriffMarks(String receiverSheriffMarks) {
        this.receiverSheriffMarks = receiverSheriffMarks;
    }

    public boolean isSheriffUserListReferred() {
        return sheriffUserListReferred;
    }

    public void setSheriffUserListReferred(boolean sheriffUserListReferred) {
        this.sheriffUserListReferred = sheriffUserListReferred;
    }

    public boolean isRecommended() {
        return recommended;
    }

    public void setRecommended(boolean recommended) {
        this.recommended = recommended;
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

    public Set<BlockedInstant> getBlockedInstants() {
        return blockedInstants;
    }

    public void setBlockedInstants(Set<BlockedInstant> blockedInstants) {
        this.blockedInstants = blockedInstants;
    }

    public void addBlockedInstant(BlockedInstant blockedInstant) {
        blockedInstants.add(blockedInstant);
        blockedInstant.setEntry(this);
    }

    public void removeBlockedInstant(BlockedInstant blockedInstant) {
        blockedInstants.removeIf(bi -> bi.getId().equals(blockedInstant.getId()));
        blockedInstant.setEntry(null);
    }

}
