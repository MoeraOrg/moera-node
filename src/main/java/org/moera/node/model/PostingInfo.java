package org.moera.node.model;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.commons.crypto.CryptoUtil;
import org.moera.node.auth.principal.AccessChecker;
import org.moera.node.auth.principal.AccessCheckers;
import org.moera.node.auth.principal.Principal;
import org.moera.node.data.EntryAttachment;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.Feed;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.OwnPosting;
import org.moera.node.data.Posting;
import org.moera.node.data.SourceFormat;
import org.moera.node.data.Story;
import org.moera.node.model.body.Body;
import org.moera.node.text.HeadingExtractor;
import org.moera.node.text.sanitizer.HtmlSanitizer;
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
    private AvatarImage receiverAvatar;
    private String receiverPostingId;
    private String parentMediaId;
    private String ownerName;
    private String ownerFullName;
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
    private byte[] signature;
    private Short signatureVersion;
    private List<FeedReference> feedReferences;
    private Map<String, Principal> operations;
    private Map<String, Principal> receiverOperations;
    private AcceptedReactions acceptedReactions;
    private ClientReactionInfo clientReaction;
    private ReactionTotalsInfo reactions;
    private List<PostingSourceInfo> sources;
    private Integer totalComments;
    private PostingSubscriptionsInfo subscriptions = new PostingSubscriptionsInfo();

    public PostingInfo() {
    }

    public PostingInfo(UUID id) {
        this.id = id.toString();
    }

    public PostingInfo(Posting posting, AccessChecker accessChecker) {
        this(posting, posting.getCurrentRevision(), false, accessChecker);
    }

    public PostingInfo(Posting posting, boolean includeSource, AccessChecker accessChecker) {
        this(posting, posting.getCurrentRevision(), includeSource, accessChecker);
    }

    public PostingInfo(Posting posting, EntryRevision revision, boolean includeSource, AccessChecker accessChecker) {
        this(posting, revision, null, includeSource, accessChecker);
    }

    public PostingInfo(Posting posting, List<Story> stories, AccessChecker accessChecker) {
        this(posting, posting.getCurrentRevision(), stories, false, accessChecker);
    }

    public PostingInfo(Posting posting, EntryRevision revision, List<Story> stories, boolean includeSource,
                       AccessChecker accessChecker) {
        id = posting.getId().toString();
        revisionId = revision.getId().toString();
        receiverRevisionId = revision.getReceiverRevisionId();
        totalRevisions = posting.getTotalRevisions();
        receiverName = posting.getReceiverName();
        receiverFullName = posting.getReceiverFullName();
        if (posting.getReceiverAvatarMediaFile() != null) {
            receiverAvatar = new AvatarImage(posting.getReceiverAvatarMediaFile(), posting.getReceiverAvatarShape());
        }
        receiverPostingId = posting.getReceiverEntryId();
        parentMediaId = posting.getParentMedia() != null ? posting.getParentMedia().getId().toString() : null;
        ownerName = posting.getOwnerName();
        ownerFullName = posting.getOwnerFullName();
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
        media = revision.getAttachments().stream()
                .sorted(Comparator.comparingInt(EntryAttachment::getOrdinal))
                .map(ea -> new MediaAttachment(ea, receiverName))
                .toArray(MediaAttachment[]::new);
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
        signature = revision.getSignature();
        signatureVersion = revision.getSignatureVersion();
        if (stories != null && !stories.isEmpty()) {
            feedReferences = stories.stream().map(FeedReference::new).collect(Collectors.toList());
        }
        operations = new HashMap<>();
        operations.put("view", posting.getViewPrincipal());
        operations.put("edit", posting.getEditPrincipal());
        operations.put("delete", posting.getDeletePrincipal());
        operations.put("viewComments", posting.getViewCommentsPrincipal());
        operations.put("addComment", posting.getAddCommentPrincipal());
        operations.put("viewReactions", posting.getViewReactionsPrincipal());
        operations.put("viewNegativeReactions", posting.getViewNegativeReactionsPrincipal());
        operations.put("viewReactionTotals", posting.getViewReactionTotalsPrincipal());
        operations.put("viewNegativeReactionTotals", posting.getViewNegativeReactionTotalsPrincipal());
        operations.put("viewReactionRatios", posting.getViewReactionRatiosPrincipal());
        operations.put("viewNegativeReactionRatios", posting.getViewNegativeReactionRatiosPrincipal());
        operations.put("addReaction", posting.getAddReactionPrincipal());
        operations.put("addNegativeReaction", posting.getAddNegativeReactionPrincipal());
        receiverOperations = new HashMap<>();
        receiverOperations.put("view", posting.getReceiverViewPrincipal());
        receiverOperations.put("edit", posting.getReceiverEditPrincipal());
        receiverOperations.put("delete", posting.getReceiverDeletePrincipal());
        receiverOperations.put("viewComments", posting.getReceiverViewCommentsPrincipal());
        receiverOperations.put("addComment", posting.getReceiverAddCommentPrincipal());
        receiverOperations.put("viewReactions", posting.getReceiverViewReactionsPrincipal());
        receiverOperations.put("viewNegativeReactions", posting.getReceiverViewNegativeReactionsPrincipal());
        receiverOperations.put("viewReactionTotals", posting.getReceiverViewReactionTotalsPrincipal());
        receiverOperations.put("viewNegativeReactionTotals", posting.getReceiverViewNegativeReactionTotalsPrincipal());
        receiverOperations.put("viewReactionRatios", posting.getReceiverViewReactionRatiosPrincipal());
        receiverOperations.put("viewNegativeReactionRatios", posting.getReceiverViewNegativeReactionRatiosPrincipal());
        receiverOperations.put("addReaction", posting.getReceiverAddReactionPrincipal());
        receiverOperations.put("addNegativeReaction", posting.getReceiverAddNegativeReactionPrincipal());
        acceptedReactions = new AcceptedReactions();
        acceptedReactions.setPositive(posting.getAcceptedReactionsPositive());
        acceptedReactions.setNegative(posting.getAcceptedReactionsNegative());
        reactions = new ReactionTotalsInfo(posting.getReactionTotals(), posting, accessChecker);
        sources = posting.getSources() != null
                ? posting.getSources().stream().map(PostingSourceInfo::new).collect(Collectors.toList())
                : Collections.emptyList();
        Principal viewComments = posting.isOriginal()
                ? posting.getViewCommentsPrincipalAbsolute()
                : posting.getReceiverViewCommentsPrincipalAbsolute();
        totalComments = accessChecker.isPrincipal(viewComments) ? posting.getTotalChildren() : 0;
        subscriptions = PostingSubscriptionsInfo.fromSubscribers(posting.getSubscribers());
    }

    public static PostingInfo forUi(Posting posting) {
        return forUi(posting, null);
    }

    public static PostingInfo forUi(Posting posting, List<Story> stories) {
        PostingInfo info = new PostingInfo(posting, stories, AccessCheckers.PUBLIC);
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

    public PostingSubscriptionsInfo getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(PostingSubscriptionsInfo subscriptions) {
        this.subscriptions = subscriptions;
    }

    public void toPickedPosting(Posting posting) {
        posting.setEditedAt(Util.toTimestamp(editedAt));
        posting.setReceiverEntryId(isOriginal() ? id : receiverPostingId);
        posting.setOwnerName(ownerName);
        posting.setOwnerFullName(ownerFullName);
        if (ownerAvatar != null && ownerAvatar.getShape() != null) {
            posting.setOwnerAvatarShape(ownerAvatar.getShape());
        }
        posting.setReceiverCreatedAt(Util.toTimestamp(isOriginal() ? createdAt : receiverCreatedAt));
        posting.setReceiverEditedAt(Util.toTimestamp(isOriginal() ? editedAt : receiverEditedAt));
        posting.setAcceptedReactionsPositive(acceptedReactions.getPositive());
        posting.setAcceptedReactionsNegative(acceptedReactions.getNegative());
        posting.setTotalChildren(totalComments);
        // TODO visibility to a particular group of friends should be converted to something here
        // https://github.com/MoeraOrg/moera-issues/issues/207
        Principal principal = getOperations().getOrDefault("view", Principal.PUBLIC);
        posting.setViewPrincipal(principal);
        posting.setReceiverViewPrincipal(principal);
        // TODO visibility to a particular group of friends should be converted to something here
        // https://github.com/MoeraOrg/moera-issues/issues/207
        principal = getOperations().getOrDefault("edit", Principal.PRIVATE);
        posting.setReceiverEditPrincipal(principal);
        principal = getOperations().getOrDefault("delete", Principal.PRIVATE);
        posting.setReceiverDeletePrincipal(principal);
        principal = getOperations().getOrDefault("viewComments", Principal.PUBLIC);
        posting.setViewCommentsPrincipal(Principal.NONE);
        posting.setReceiverViewCommentsPrincipal(principal);
        principal = getOperations().getOrDefault("addComment", Principal.PUBLIC);
        posting.setAddCommentPrincipal(Principal.NONE);
        posting.setReceiverAddCommentPrincipal(principal);
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
        principal = getOperations().getOrDefault("addReaction", Principal.PUBLIC);
        posting.setAddReactionPrincipal(Principal.NONE);
        posting.setReceiverAddReactionPrincipal(principal);
        principal = getOperations().getOrDefault("addNegativeReaction", Principal.PUBLIC);
        posting.setAddNegativeReactionPrincipal(Principal.NONE);
        posting.setReceiverAddNegativeReactionPrincipal(principal);
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
        entryRevision.setDescription(HeadingExtractor.extractDescription(body));
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
