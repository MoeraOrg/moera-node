package org.moera.node.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.commons.crypto.CryptoUtil;
import org.moera.node.auth.Scope;
import org.moera.node.auth.principal.AccessChecker;
import org.moera.node.auth.principal.AccessCheckers;
import org.moera.node.auth.principal.Principal;
import org.moera.node.data.BlockedOperation;
import org.moera.node.data.Entry;
import org.moera.node.data.EntryAttachment;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.Feed;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.OwnPosting;
import org.moera.node.data.SheriffMark;
import org.moera.node.data.SourceFormat;
import org.moera.node.data.Story;
import org.moera.node.model.body.Body;
import org.moera.node.operations.FeedOperations;
import org.moera.node.operations.MediaAttachmentsProvider;
import org.moera.node.option.Options;
import org.moera.node.text.HeadingExtractor;
import org.moera.node.text.sanitizer.HtmlSanitizer;
import org.moera.node.util.SheriffUtil;
import org.moera.node.util.Util;
import org.springframework.util.ObjectUtils;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostingInfo implements MediaInfo, ReactionsInfo {

    private String id;
    private String revisionId;
    private String receiverRevisionId;
    private Integer totalRevisions;
    private String receiverName;
    private String receiverFullName;
    private String receiverGender;
    private AvatarImage receiverAvatar;
    private String receiverPostingId;
    private String parentMediaId;
    private String ownerName;
    private String ownerFullName;
    private String ownerGender;
    private AvatarImage ownerAvatar;
    private Body bodyPreview;

    @JsonIgnore
    private String saneBodyPreview;

    private Body bodySrc;
    private byte[] bodySrcHash;
    private SourceFormat bodySrcFormat;
    private Body body;

    @JsonIgnore
    private String saneBody;

    private String bodyFormat;
    private MediaAttachment[] media;
    private String heading;
    private UpdateInfo updateInfo;
    private Long createdAt;
    private Long editedAt;
    private Long deletedAt;
    private Long receiverCreatedAt;
    private Long receiverEditedAt;
    private Long receiverDeletedAt;
    private Long revisionCreatedAt;
    private Long receiverRevisionCreatedAt;
    private Long deadline;
    private byte[] digest;
    private byte[] signature;
    private Short signatureVersion;
    private List<FeedReference> feedReferences;
    private List<BlockedPostingInstantInfo> blockedInstants;
    private Map<String, Principal> operations;
    private Map<String, Principal> receiverOperations;
    private Map<String, Principal> commentOperations;
    private Map<String, Principal> reactionOperations;
    private Map<String, Principal> commentReactionOperations;
    private Set<String> blockedOperations;
    private Set<String> blockedCommentOperations;
    private List<String> sheriffs;
    private List<SheriffMark> sheriffMarks;

    @JsonIgnore
    private boolean sheriffUserListReferred;

    private AcceptedReactions acceptedReactions;
    private ClientReactionInfo clientReaction;
    private ReactionTotalsInfo reactions;
    private List<PostingSourceInfo> sources;
    private Integer totalComments;

    public PostingInfo() {
    }

    // for liberin models
    public PostingInfo(Entry posting, AccessChecker accessChecker) {
        this(
                posting,
                posting.getCurrentRevision(),
                null,
                MediaAttachmentsProvider.RELATIONS,
                false,
                accessChecker,
                null);
    }

    public PostingInfo(Entry posting, MediaAttachmentsProvider mediaAttachmentsProvider, AccessChecker accessChecker) {
        this(
                posting,
                posting.getCurrentRevision(),
                null,
                mediaAttachmentsProvider,
                false,
                accessChecker,
                null);
    }

    // for postings attached to media
    public PostingInfo(Entry posting, boolean includeSource, AccessChecker accessChecker) {
        this(
                posting,
                posting.getCurrentRevision(),
                null,
                MediaAttachmentsProvider.NONE,
                includeSource,
                accessChecker,
                null);
    }

    // for fingerprints
    public PostingInfo(Entry posting, EntryRevision revision, boolean includeSource, AccessChecker accessChecker) {
        this(
                posting,
                revision,
                null,
                MediaAttachmentsProvider.RELATIONS,
                includeSource,
                accessChecker,
                null);
    }

    public PostingInfo(Entry posting, Collection<Story> stories, MediaAttachmentsProvider mediaAttachmentsProvider,
                       AccessChecker accessChecker, Options options) {
        this(
                posting,
                posting.getCurrentRevision(),
                stories,
                mediaAttachmentsProvider,
                false,
                accessChecker,
                options);
    }

    public PostingInfo(Entry posting, Collection<Story> stories, MediaAttachmentsProvider mediaAttachmentsProvider,
                       boolean includeSource, AccessChecker accessChecker, Options options) {
        this(
                posting,
                posting.getCurrentRevision(),
                stories,
                mediaAttachmentsProvider,
                includeSource,
                accessChecker,
                options);
    }

    public PostingInfo(Entry posting, EntryRevision revision, Collection<Story> stories,
                       MediaAttachmentsProvider mediaAttachmentsProvider, boolean includeSource,
                       AccessChecker accessChecker, Options options) {
        id = posting.getId().toString();
        revisionId = revision.getId().toString();
        receiverRevisionId = revision.getReceiverRevisionId();
        totalRevisions = posting.getTotalRevisions();
        receiverName = posting.getReceiverName();
        receiverFullName = posting.getReceiverFullName();
        receiverGender = posting.getReceiverGender();
        if (posting.getReceiverAvatarMediaFile() != null) {
            receiverAvatar = new AvatarImage(posting.getReceiverAvatarMediaFile(), posting.getReceiverAvatarShape());
        }
        receiverPostingId = posting.getReceiverEntryId();
        parentMediaId = posting.getParentMedia() != null ? posting.getParentMedia().getId().toString() : null;
        ownerName = posting.getOwnerName();
        ownerFullName = posting.getOwnerFullName();
        ownerGender = posting.getOwnerGender();
        if (posting.getOwnerAvatarMediaFile() != null) {
            ownerAvatar = new AvatarImage(posting.getOwnerAvatarMediaFile(), posting.getOwnerAvatarShape());
        }
        bodyPreview = new Body(revision.getBodyPreview());
        if (includeSource && !ObjectUtils.isEmpty(revision.getBodySrc())) {
            bodySrc = new Body(revision.getBodySrc());
        }
        bodySrcHash = revision.getReceiverBodySrcHash() != null
                ? revision.getReceiverBodySrcHash()
                : CryptoUtil.digest(revision.getBodySrc());
        bodySrcFormat = revision.getBodySrcFormat();
        body = new Body(revision.getBody());
        bodyFormat = revision.getBodyFormat();
        media = mediaAttachmentsProvider.getMediaAttachments(revision, receiverName);
        heading = revision.getHeading();
        if (!UpdateInfo.isEmpty(revision)) {
            updateInfo = new UpdateInfo(revision);
        }
        createdAt = Util.toEpochSecond(posting.getCreatedAt());
        editedAt = Util.toEpochSecond(posting.getEditedAt());
        deletedAt = Util.toEpochSecond(posting.getDeletedAt());
        receiverCreatedAt = Util.toEpochSecond(posting.getReceiverCreatedAt());
        receiverEditedAt = Util.toEpochSecond(posting.getReceiverEditedAt());
        receiverDeletedAt = Util.toEpochSecond(posting.getReceiverDeletedAt());
        revisionCreatedAt = Util.toEpochSecond(revision.getCreatedAt());
        receiverRevisionCreatedAt = Util.toEpochSecond(revision.getReceiverCreatedAt());
        deadline = Util.toEpochSecond(posting.getDeadline());
        digest = revision.getDigest();
        signature = revision.getSignature();
        signatureVersion = revision.getSignatureVersion();
        if (!ObjectUtils.isEmpty(stories)) {
            feedReferences = stories.stream().map(FeedReference::new).collect(Collectors.toList());
        }
        if (accessChecker.isPrincipal(Principal.ADMIN, Scope.OTHER)
                && posting.getBlockedInstants() != null && !posting.getBlockedInstants().isEmpty()) {
            blockedInstants = posting.getBlockedInstants().stream()
                    .map(BlockedPostingInstantInfo::new)
                    .collect(Collectors.toList());
        }

        operations = new HashMap<>();
        putOperation(operations, "view",
                posting.getViewPrincipal(), Principal.PUBLIC);
        putOperation(operations, "edit",
                posting.getEditPrincipal(), Principal.OWNER);
        putOperation(operations, "delete",
                posting.getDeletePrincipal(), Principal.PRIVATE);
        putOperation(operations, "viewComments",
                posting.getViewCommentsPrincipal(), Principal.PUBLIC);
        putOperation(operations, "addComment",
                posting.getAddCommentPrincipal(), Principal.SIGNED);
        putOperation(operations, "overrideComment",
                posting.getOverrideCommentPrincipal(), Principal.OWNER);
        putOperation(operations, "viewReactions",
                posting.getViewReactionsPrincipal(), Principal.PUBLIC);
        putOperation(operations, "viewNegativeReactions",
                posting.getViewNegativeReactionsPrincipal(), Principal.PUBLIC);
        putOperation(operations, "viewReactionTotals",
                posting.getViewReactionTotalsPrincipal(), Principal.PUBLIC);
        putOperation(operations, "viewNegativeReactionTotals",
                posting.getViewNegativeReactionTotalsPrincipal(), Principal.PUBLIC);
        putOperation(operations, "viewReactionRatios",
                posting.getViewReactionRatiosPrincipal(), Principal.PUBLIC);
        putOperation(operations, "viewNegativeReactionRatios",
                posting.getViewNegativeReactionRatiosPrincipal(), Principal.PUBLIC);
        putOperation(operations, "addReaction",
                posting.getAddReactionPrincipal(), Principal.SIGNED);
        putOperation(operations, "addNegativeReaction",
                posting.getAddNegativeReactionPrincipal(), Principal.SIGNED);
        putOperation(operations, "overrideReaction",
                posting.getOverrideReactionPrincipal(), Principal.OWNER);
        putOperation(operations, "overrideCommentReaction",
                posting.getOverrideCommentReactionPrincipal(), Principal.OWNER);

        if (!posting.isOriginal()) {
            receiverOperations = new HashMap<>();
            putOperation(receiverOperations, "view",
                    posting.getReceiverViewPrincipal(), Principal.PUBLIC);
            putOperation(receiverOperations, "edit",
                    posting.getReceiverEditPrincipal(), Principal.OWNER);
            putOperation(receiverOperations, "delete",
                    posting.getReceiverDeletePrincipal(), Principal.PRIVATE);
            putOperation(receiverOperations, "viewComments",
                    posting.getReceiverViewCommentsPrincipal(), Principal.PUBLIC);
            putOperation(receiverOperations, "addComment",
                    posting.getReceiverAddCommentPrincipal(), Principal.SIGNED);
            putOperation(receiverOperations, "overrideComment",
                    posting.getReceiverOverrideCommentPrincipal(), Principal.OWNER);
            putOperation(receiverOperations, "viewReactions",
                    posting.getReceiverViewReactionsPrincipal(), Principal.PUBLIC);
            putOperation(receiverOperations, "viewNegativeReactions",
                    posting.getReceiverViewNegativeReactionsPrincipal(), Principal.PUBLIC);
            putOperation(receiverOperations, "viewReactionTotals",
                    posting.getReceiverViewReactionTotalsPrincipal(), Principal.PUBLIC);
            putOperation(receiverOperations, "viewNegativeReactionTotals",
                    posting.getReceiverViewNegativeReactionTotalsPrincipal(), Principal.PUBLIC);
            putOperation(receiverOperations, "viewReactionRatios",
                    posting.getReceiverViewReactionRatiosPrincipal(), Principal.PUBLIC);
            putOperation(receiverOperations, "viewNegativeReactionRatios",
                    posting.getReceiverViewNegativeReactionRatiosPrincipal(), Principal.PUBLIC);
            putOperation(receiverOperations, "addReaction",
                    posting.getReceiverAddReactionPrincipal(), Principal.SIGNED);
            putOperation(receiverOperations, "addNegativeReaction",
                    posting.getReceiverAddNegativeReactionPrincipal(), Principal.SIGNED);
            putOperation(receiverOperations, "overrideReaction",
                    posting.getReceiverOverrideReactionPrincipal(), Principal.OWNER);
            putOperation(receiverOperations, "overrideCommentReaction",
                    posting.getReceiverOverrideCommentReactionPrincipal(), Principal.OWNER);
        }

        commentOperations = new HashMap<>();
        putOperation(commentOperations, "view",
                posting.getChildOperations().getView(), Principal.UNSET);
        putOperation(commentOperations, "edit",
                posting.getChildOperations().getEdit(), Principal.UNSET);
        putOperation(commentOperations, "delete",
                posting.getChildOperations().getDelete(), Principal.UNSET);
        putOperation(commentOperations, "viewReactions",
                posting.getChildOperations().getViewReactions(), Principal.UNSET);
        putOperation(commentOperations, "viewNegativeReactions",
                posting.getChildOperations().getViewNegativeReactions(), Principal.UNSET);
        putOperation(commentOperations, "viewReactionTotals",
                posting.getChildOperations().getViewReactionTotals(), Principal.UNSET);
        putOperation(commentOperations, "viewNegativeReactionTotals",
                posting.getChildOperations().getViewNegativeReactionTotals(), Principal.UNSET);
        putOperation(commentOperations, "viewReactionRatios",
                posting.getChildOperations().getViewReactionRatios(), Principal.UNSET);
        putOperation(commentOperations, "viewNegativeReactionRatios",
                posting.getChildOperations().getViewNegativeReactionRatios(), Principal.UNSET);
        putOperation(commentOperations, "addReaction",
                posting.getChildOperations().getAddReaction(), Principal.UNSET);
        putOperation(commentOperations, "addNegativeReaction",
                posting.getChildOperations().getAddNegativeReaction(), Principal.UNSET);

        reactionOperations = new HashMap<>();
        putOperation(reactionOperations, "view",
                posting.getReactionOperations().getView(), Principal.UNSET);
        putOperation(reactionOperations, "delete",
                posting.getReactionOperations().getDelete(), Principal.UNSET);

        commentReactionOperations = new HashMap<>();
        putOperation(commentReactionOperations, "view",
                posting.getChildReactionOperations().getView(), Principal.UNSET);
        putOperation(commentReactionOperations, "delete",
                posting.getChildReactionOperations().getDelete(), Principal.UNSET);

        fillSheriffs(posting, options);
        sheriffUserListReferred = posting.isSheriffUserListReferred();

        acceptedReactions = new AcceptedReactions();
        acceptedReactions.setPositive(posting.getAcceptedReactionsPositive());
        acceptedReactions.setNegative(posting.getAcceptedReactionsNegative());

        reactions = new ReactionTotalsInfo(posting.getReactionTotals(), posting, accessChecker);
        sources = posting.getSources() != null
                ? posting.getSources().stream().map(PostingSourceInfo::new).collect(Collectors.toList())
                : Collections.emptyList();
        Principal viewComments = posting.isOriginal()
                ? posting.getViewCommentsE()
                : posting.getReceiverViewCommentsE();
        totalComments = accessChecker.isPrincipal(viewComments, Scope.VIEW_CONTENT) ? posting.getTotalChildren() : 0;
    }

    private static void putOperation(Map<String, Principal> operations, String operationName, Principal value,
                                     Principal defaultValue) {
        if (value != null && !value.equals(defaultValue)) {
            operations.put(operationName, value);
        }
    }

    private void fillSheriffs(Entry posting, Options options) {
        if (posting.isOriginal()) {
            fillFeedSheriffs(options);
            SheriffUtil.deserializeSheriffMarks(posting.getSheriffMarks()).ifPresent(marks -> {
                if (sheriffMarks == null) {
                    sheriffMarks = new ArrayList<>();
                }
                sheriffMarks.addAll(marks);
            });
        } else {
            sheriffs = SheriffUtil.deserializeSheriffs(posting.getReceiverSheriffs()).orElse(null);
            sheriffMarks = SheriffUtil.deserializeSheriffMarks(posting.getReceiverSheriffMarks()).orElse(null);
        }
    }

    private void fillFeedSheriffs(Options options) {
        if (feedReferences == null || options == null) {
            return;
        }

        for (FeedReference feedReference : feedReferences) {
            String feedName = feedReference.getFeedName();

            FeedOperations.getFeedSheriffs(options, feedName).ifPresent(feedSheriffs -> {
                if (sheriffs == null) {
                    sheriffs = new ArrayList<>();
                }
                sheriffs.addAll(feedSheriffs);
            });

            FeedOperations.getFeedSheriffMarks(options, feedName).ifPresent(marks -> {
                if (sheriffMarks == null) {
                    sheriffMarks = new ArrayList<>();
                }
                sheriffMarks.addAll(marks);
            });
        }
    }

    public static PostingInfo forUi(Entry posting, MediaAttachmentsProvider mediaAttachmentsProvider) {
        return forUi(posting, null, mediaAttachmentsProvider, null);
    }

    public static PostingInfo forUi(Entry posting, List<Story> stories,
                                    MediaAttachmentsProvider mediaAttachmentsProvider, Options options) {
        PostingInfo info = new PostingInfo(posting, stories, mediaAttachmentsProvider, AccessCheckers.PUBLIC, options);
        String saneBodyPreview = posting.getCurrentRevision().getSaneBodyPreview();
        if (saneBodyPreview != null) {
            info.setSaneBodyPreview(saneBodyPreview);
        } else {
            info.setSaneBodyPreview(!ObjectUtils.isEmpty(
                    info.getBodyPreview().getText()) ? info.getBodyPreview().getText() : info.getBody().getText());
        }
        String saneBody = posting.getCurrentRevision().getSaneBody();
        info.setSaneBody(saneBody != null ? saneBody : info.getBody().getText());
        return info;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRevisionId() {
        return revisionId;
    }

    public void setRevisionId(String revisionId) {
        this.revisionId = revisionId;
    }

    public String getReceiverRevisionId() {
        return receiverRevisionId;
    }

    public void setReceiverRevisionId(String receiverRevisionId) {
        this.receiverRevisionId = receiverRevisionId;
    }

    public Integer getTotalRevisions() {
        return totalRevisions;
    }

    public void setTotalRevisions(Integer totalRevisions) {
        this.totalRevisions = totalRevisions;
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

    public String getReceiverGender() {
        return receiverGender;
    }

    public void setReceiverGender(String receiverGender) {
        this.receiverGender = receiverGender;
    }

    public AvatarImage getReceiverAvatar() {
        return receiverAvatar;
    }

    public void setReceiverAvatar(AvatarImage receiverAvatar) {
        this.receiverAvatar = receiverAvatar;
    }

    @JsonIgnore
    public boolean isOriginal() {
        return getReceiverName() == null;
    }

    public String getReceiverPostingId() {
        return receiverPostingId;
    }

    public void setReceiverPostingId(String receiverPostingId) {
        this.receiverPostingId = receiverPostingId;
    }

    public String getParentMediaId() {
        return parentMediaId;
    }

    public void setParentMediaId(String parentMediaId) {
        this.parentMediaId = parentMediaId;
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

    public Body getBodyPreview() {
        return bodyPreview;
    }

    public void setBodyPreview(Body bodyPreview) {
        this.bodyPreview = bodyPreview;
    }

    public String getSaneBodyPreview() {
        return saneBodyPreview;
    }

    public void setSaneBodyPreview(String saneBodyPreview) {
        this.saneBodyPreview = saneBodyPreview;
    }

    public Body getBodySrc() {
        return bodySrc;
    }

    public void setBodySrc(Body bodySrc) {
        this.bodySrc = bodySrc;
    }

    public byte[] getBodySrcHash() {
        return bodySrcHash;
    }

    public void setBodySrcHash(byte[] bodySrcHash) {
        this.bodySrcHash = bodySrcHash;
    }

    public SourceFormat getBodySrcFormat() {
        return bodySrcFormat;
    }

    public void setBodySrcFormat(SourceFormat bodySrcFormat) {
        this.bodySrcFormat = bodySrcFormat;
    }

    public Body getBody() {
        return body;
    }

    public void setBody(Body body) {
        this.body = body;
    }

    public String getSaneBody() {
        return saneBody;
    }

    public void setSaneBody(String saneBody) {
        this.saneBody = saneBody;
    }

    public String getBodyFormat() {
        return bodyFormat;
    }

    public void setBodyFormat(String bodyFormat) {
        this.bodyFormat = bodyFormat;
    }

    @Override
    public MediaAttachment[] getMedia() {
        return media;
    }

    public void setMedia(MediaAttachment[] media) {
        this.media = media;
    }

    public String getHeading() {
        return heading;
    }

    public void setHeading(String heading) {
        this.heading = heading;
    }

    public UpdateInfo getUpdateInfo() {
        return updateInfo;
    }

    public void setUpdateInfo(UpdateInfo updateInfo) {
        this.updateInfo = updateInfo;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getEditedAt() {
        return editedAt;
    }

    public void setEditedAt(Long editedAt) {
        this.editedAt = editedAt;
    }

    public Long getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Long deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Long getReceiverCreatedAt() {
        return receiverCreatedAt;
    }

    public void setReceiverCreatedAt(Long receiverCreatedAt) {
        this.receiverCreatedAt = receiverCreatedAt;
    }

    public Long getReceiverEditedAt() {
        return receiverEditedAt;
    }

    public void setReceiverEditedAt(Long receiverEditedAt) {
        this.receiverEditedAt = receiverEditedAt;
    }

    public Long getReceiverDeletedAt() {
        return receiverDeletedAt;
    }

    public void setReceiverDeletedAt(Long receiverDeletedAt) {
        this.receiverDeletedAt = receiverDeletedAt;
    }

    public Long getRevisionCreatedAt() {
        return revisionCreatedAt;
    }

    public void setRevisionCreatedAt(Long revisionCreatedAt) {
        this.revisionCreatedAt = revisionCreatedAt;
    }

    public Long getReceiverRevisionCreatedAt() {
        return receiverRevisionCreatedAt;
    }

    public void setReceiverRevisionCreatedAt(Long receiverRevisionCreatedAt) {
        this.receiverRevisionCreatedAt = receiverRevisionCreatedAt;
    }

    public Long getDeadline() {
        return deadline;
    }

    public void setDeadline(Long deadline) {
        this.deadline = deadline;
    }

    public byte[] getDigest() {
        return digest;
    }

    public void setDigest(byte[] digest) {
        this.digest = digest;
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

    public List<FeedReference> getFeedReferences() {
        return feedReferences;
    }

    public void setFeedReferences(List<FeedReference> feedReferences) {
        this.feedReferences = feedReferences;
    }

    public FeedReference getFeedReference(String feedName) {
        if (getFeedReferences() == null) {
            return null;
        }
        return getFeedReferences().stream().filter(fr -> fr.getFeedName().equals(feedName)).findFirst().orElse(null);
    }

    public List<BlockedPostingInstantInfo> getBlockedInstants() {
        return blockedInstants;
    }

    public void setBlockedInstants(List<BlockedPostingInstantInfo> blockedInstants) {
        this.blockedInstants = blockedInstants;
    }
// Methods for Web UI

    @JsonIgnore
    public Long getTimelinePublishedAt() {
        FeedReference fr = getFeedReference(Feed.TIMELINE);
        return fr != null ? fr.getPublishedAt() : null;
    }

    @JsonIgnore
    public Boolean isTimelinePinned() {
        FeedReference fr = getFeedReference(Feed.TIMELINE);
        return fr != null ? fr.isPinned() : null;
    }

    @JsonIgnore
    public Long getTimelineMoment() {
        FeedReference fr = getFeedReference(Feed.TIMELINE);
        return fr != null ? fr.getMoment() : null;
    }

    // end

    @Override
    public Map<String, Principal> getOperations() {
        return operations;
    }

    public void setOperations(Map<String, Principal> operations) {
        this.operations = operations;
    }

    public Map<String, Principal> getReceiverOperations() {
        return receiverOperations;
    }

    public void setReceiverOperations(Map<String, Principal> receiverOperations) {
        this.receiverOperations = receiverOperations;
    }

    public Map<String, Principal> getCommentOperations() {
        return commentOperations;
    }

    public void setCommentOperations(Map<String, Principal> commentOperations) {
        this.commentOperations = commentOperations;
    }

    public Map<String, Principal> getReactionOperations() {
        return reactionOperations;
    }

    public void setReactionOperations(Map<String, Principal> reactionOperations) {
        this.reactionOperations = reactionOperations;
    }

    public Map<String, Principal> getCommentReactionOperations() {
        return commentReactionOperations;
    }

    public void setCommentReactionOperations(Map<String, Principal> commentReactionOperations) {
        this.commentReactionOperations = commentReactionOperations;
    }

    public Set<String> getBlockedOperations() {
        return blockedOperations;
    }

    public void setBlockedOperations(Set<String> blockedOperations) {
        this.blockedOperations = blockedOperations;
    }

    public Set<String> getBlockedCommentOperations() {
        return blockedCommentOperations;
    }

    public void setBlockedCommentOperations(Set<String> blockedCommentOperations) {
        this.blockedCommentOperations = blockedCommentOperations;
    }

    public void putBlockedOperation(BlockedOperation operation) {
        if (blockedOperations == null) {
            blockedOperations = new HashSet<>();
        }
        switch (operation) {
            case COMMENT:
                blockedOperations.add("addComment");
                break;
            case REACTION:
                blockedOperations.add("addReaction");
                if (blockedCommentOperations == null) {
                    blockedCommentOperations = new HashSet<>();
                }
                blockedCommentOperations.add("addReaction");
                break;
        }
    }

    public void putBlockedOperations(List<BlockedOperation> operations) {
        if (operations != null) {
            for (BlockedOperation operation : operations) {
                putBlockedOperation(operation);
            }
        }
    }

    public List<String> getSheriffs() {
        return sheriffs;
    }

    public void setSheriffs(List<String> sheriffs) {
        this.sheriffs = sheriffs;
    }

    public List<SheriffMark> getSheriffMarks() {
        return sheriffMarks;
    }

    public void setSheriffMarks(List<SheriffMark> sheriffMarks) {
        this.sheriffMarks = sheriffMarks;
    }

    public boolean isSheriffUserListReferred() {
        return sheriffUserListReferred;
    }

    public void setSheriffUserListReferred(boolean sheriffUserListReferred) {
        this.sheriffUserListReferred = sheriffUserListReferred;
    }

    public AcceptedReactions getAcceptedReactions() {
        return acceptedReactions;
    }

    public void setAcceptedReactions(AcceptedReactions acceptedReactions) {
        this.acceptedReactions = acceptedReactions;
    }

    public ClientReactionInfo getClientReaction() {
        return clientReaction;
    }

    public void setClientReaction(ClientReactionInfo clientReaction) {
        this.clientReaction = clientReaction;
    }

    @Override
    public ReactionTotalsInfo getReactions() {
        return reactions;
    }

    public void setReactions(ReactionTotalsInfo reactions) {
        this.reactions = reactions;
    }

    public List<PostingSourceInfo> getSources() {
        return sources;
    }

    public void setSources(List<PostingSourceInfo> sources) {
        this.sources = sources;
    }

    public Integer getTotalComments() {
        return totalComments;
    }

    public void setTotalComments(Integer totalComments) {
        this.totalComments = totalComments;
    }

    public void toPickedPosting(Entry posting) {
        posting.setEditedAt(Util.toTimestamp(editedAt));
        posting.setReceiverEntryId(isOriginal() ? id : receiverPostingId);
        posting.setOwnerName(ownerName);
        posting.setOwnerFullName(ownerFullName);
        posting.setOwnerGender(ownerGender);
        if (ownerAvatar != null && ownerAvatar.getShape() != null) {
            posting.setOwnerAvatarShape(ownerAvatar.getShape());
        }
        posting.setReceiverCreatedAt(Util.toTimestamp(isOriginal() ? createdAt : receiverCreatedAt));
        posting.setReceiverEditedAt(Util.toTimestamp(isOriginal() ? editedAt : receiverEditedAt));
        posting.setReceiverDeletedAt(null);
        posting.setAcceptedReactionsPositive(acceptedReactions.getPositive());
        posting.setAcceptedReactionsNegative(acceptedReactions.getNegative());
        posting.setTotalChildren(totalComments);
        // TODO visibility to a particular group of friends should be converted to something here
        // https://github.com/MoeraOrg/moera-issues/issues/207
        Principal principal = getOperations().getOrDefault("view", Principal.PUBLIC);
        posting.setViewPrincipal(principal.isFriends() || principal.isSubscribed() ? Principal.PRIVATE : principal);
        posting.setReceiverViewPrincipal(principal);
        // TODO visibility to a particular group of friends should be converted to something here
        // https://github.com/MoeraOrg/moera-issues/issues/207
        principal = getOperations().getOrDefault("edit", Principal.OWNER);
        posting.setReceiverEditPrincipal(principal);
        principal = getOperations().getOrDefault("delete", Principal.PRIVATE);
        posting.setReceiverDeletePrincipal(principal);
        principal = getOperations().getOrDefault("viewComments", Principal.PUBLIC);
        posting.setViewCommentsPrincipal(Principal.NONE);
        posting.setReceiverViewCommentsPrincipal(principal);
        principal = getOperations().getOrDefault("addComment", Principal.SIGNED);
        posting.setAddCommentPrincipal(Principal.NONE);
        posting.setReceiverAddCommentPrincipal(principal);
        principal = getOperations().getOrDefault("overrideComment", Principal.OWNER);
        posting.setReceiverOverrideCommentPrincipal(principal);
        principal = getOperations().getOrDefault("viewReactions", Principal.PUBLIC);
        posting.setViewReactionsPrincipal(Principal.NONE);
        posting.setReceiverViewReactionsPrincipal(principal);
        principal = getOperations().getOrDefault("viewNegativeReactions", Principal.PUBLIC);
        posting.setViewNegativeReactionsPrincipal(Principal.NONE);
        posting.setReceiverViewNegativeReactionsPrincipal(principal);
        principal = getOperations().getOrDefault("viewReactionTotals", Principal.PUBLIC);
        posting.setViewReactionTotalsPrincipal(principal);
        posting.setReceiverViewReactionTotalsPrincipal(principal);
        principal = getOperations().getOrDefault("viewNegativeReactionTotals", Principal.PUBLIC);
        posting.setViewNegativeReactionTotalsPrincipal(principal);
        posting.setReceiverViewNegativeReactionTotalsPrincipal(principal);
        principal = getOperations().getOrDefault("viewReactionRatios", Principal.PUBLIC);
        posting.setViewReactionRatiosPrincipal(principal);
        posting.setReceiverViewReactionRatiosPrincipal(principal);
        principal = getOperations().getOrDefault("viewNegativeReactionRatios", Principal.PUBLIC);
        posting.setViewNegativeReactionRatiosPrincipal(principal);
        posting.setReceiverViewNegativeReactionRatiosPrincipal(principal);
        principal = getOperations().getOrDefault("addReaction", Principal.SIGNED);
        posting.setAddReactionPrincipal(Principal.NONE);
        posting.setReceiverAddReactionPrincipal(principal);
        principal = getOperations().getOrDefault("addNegativeReaction", Principal.SIGNED);
        posting.setAddNegativeReactionPrincipal(Principal.NONE);
        posting.setReceiverAddNegativeReactionPrincipal(principal);
        principal = getOperations().getOrDefault("overrideReaction", Principal.OWNER);
        posting.setReceiverOverrideReactionPrincipal(principal);
        principal = getOperations().getOrDefault("overrideCommentReaction", Principal.OWNER);
        posting.setReceiverOverrideCommentReactionPrincipal(principal);
        posting.setReceiverSheriffs(SheriffUtil.serializeSheriffs(sheriffs).orElse(null));
        posting.setReceiverSheriffMarks(SheriffUtil.serializeSheriffMarks(sheriffMarks).orElse(null));
    }

    public void toPickedEntryRevision(EntryRevision entryRevision) {
        List<MediaFileOwner> media = entryRevision.getAttachments().stream()
                .map(EntryAttachment::getMediaFileOwner)
                .collect(Collectors.toList());

        entryRevision.setReceiverRevisionId(revisionId);
        entryRevision.setBodyPreview(bodyPreview.getEncoded());
        entryRevision.setSaneBodyPreview(HtmlSanitizer.sanitizeIfNeeded(
                !ObjectUtils.isEmpty(bodyPreview.getText()) ? bodyPreview : body, true, media));
        entryRevision.setBodySrcFormat(bodySrcFormat);
        entryRevision.setReceiverBodySrcHash(bodySrcHash);
        entryRevision.setBodyFormat(bodyFormat);
        entryRevision.setBody(body.getEncoded());
        entryRevision.setSaneBody(HtmlSanitizer.sanitizeIfNeeded(body, false, media));
        entryRevision.setHeading(heading);
        entryRevision.setDescription(HeadingExtractor.extractDescription(body, false, heading));
        if (deletedAt != null) {
            entryRevision.setDeletedAt(Util.now());
        }
        entryRevision.setReceiverCreatedAt(Util.toTimestamp(revisionCreatedAt));
        entryRevision.setReceiverDeletedAt(Util.toTimestamp(deletedAt));
        entryRevision.setSignature(signature);
        entryRevision.setSignatureVersion(signatureVersion);
    }

    public void toOwnPosting(OwnPosting ownPosting) {
        ownPosting.setRemotePostingId(id);
        ownPosting.setHeading(heading);
        ownPosting.setCreatedAt(Util.now());
    }

}
