package org.moera.node.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.commons.crypto.CryptoUtil;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.Feed;
import org.moera.node.data.Posting;
import org.moera.node.data.SourceFormat;
import org.moera.node.data.Story;
import org.moera.node.util.Util;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostingInfo {

    private String id;
    private String revisionId;
    private String receiverRevisionId;
    private Integer totalRevisions;
    private String receiverName;
    private String receiverPostingId;
    private String ownerName;
    private Body bodyPreview;
    private Body bodySrc;
    private byte[] bodySrcHash;
    private SourceFormat bodySrcFormat;
    private Body body;
    private String bodyFormat;
    private String heading;
    private Long createdAt;
    private Long editedAt;
    private Long deletedAt;
    private Long receiverCreatedAt;
    private Long receiverEditedAt;
    private Long receiverDeletedAt;
    private Long deadline;
    private Boolean draft;
    private Boolean draftPending;
    private byte[] signature;
    private Short signatureVersion;
    private List<FeedReference> feedReferences;
    private Map<String, String[]> operations;
    private AcceptedReactions acceptedReactions;
    private ClientReactionInfo clientReaction;
    private ReactionTotalsInfo reactions;
    private Boolean reactionsVisible;
    private Boolean reactionTotalsVisible;
    private List<PostingSourceInfo> sources;

    public PostingInfo() {
    }

    public PostingInfo(UUID id) {
        this.id = id.toString();
    }

    public PostingInfo(Posting posting, boolean isAdminOrOwner) {
        this(posting, posting.getCurrentRevision(), false, isAdminOrOwner);
    }

    public PostingInfo(Posting posting, boolean includeSource, boolean isAdminOrOwner) {
        this(posting, posting.getCurrentRevision(), includeSource, isAdminOrOwner);
    }

    public PostingInfo(Posting posting, EntryRevision revision, boolean includeSource, boolean isAdminOrOwner) {
        this(posting, revision, null, includeSource, isAdminOrOwner);
    }

    public PostingInfo(Posting posting, List<Story> stories, boolean isAdminOrOwner) {
        this(posting, posting.getCurrentRevision(), stories, false, isAdminOrOwner);
    }

    public PostingInfo(Posting posting, EntryRevision revision, List<Story> stories, boolean includeSource,
                       boolean isAdminOrOwner) {
        id = posting.getId().toString();
        revisionId = revision.getId().toString();
        receiverRevisionId = revision.getReceiverRevisionId();
        totalRevisions = posting.getTotalRevisions();
        receiverName = posting.getReceiverName();
        receiverPostingId = posting.getReceiverEntryId();
        ownerName = posting.getOwnerName();
        bodyPreview = new Body(revision.getBodyPreview());
        if (includeSource) {
            bodySrc = new Body(revision.getBodySrc());
        }
        bodySrcHash = revision.getReceiverBodySrcHash() != null
                ? revision.getReceiverBodySrcHash()
                : CryptoUtil.digest(revision.getBodySrc());
        bodySrcFormat = revision.getBodySrcFormat();
        body = new Body(revision.getBody());
        bodyFormat = revision.getBodyFormat();
        heading = revision.getHeading();
        createdAt = Util.toEpochSecond(posting.getCreatedAt());
        editedAt = Util.toEpochSecond(revision.getCreatedAt());
        deletedAt = Util.toEpochSecond(posting.getDeletedAt());
        receiverCreatedAt = Util.toEpochSecond(posting.getReceiverCreatedAt());
        receiverEditedAt = Util.toEpochSecond(revision.getReceiverCreatedAt());
        receiverDeletedAt = Util.toEpochSecond(revision.getReceiverDeletedAt());
        deadline = Util.toEpochSecond(posting.getDeadline());
        if (posting.isDraft()) {
            draft = true;
        }
        if (includeSource && isAdminOrOwner) {
            draftPending = posting.getDraftRevision() != null;
        }
        signature = revision.getSignature();
        signatureVersion = revision.getSignatureVersion();
        if (stories != null && !stories.isEmpty()) {
            feedReferences = stories.stream().map(FeedReference::new).collect(Collectors.toList());
        }
        operations = new HashMap<>();
        operations.put("edit", receiverName == null ? new String[]{"owner"} : new String[0]);
        operations.put("delete", receiverName == null ? new String[]{"owner", "admin"} : new String[]{"admin"});
        operations.put("revisions", new String[0]);
        operations.put("reactions",
                posting.isReactionsVisible() ? new String[]{"public"} : new String[]{"owner", "admin"});
        acceptedReactions = new AcceptedReactions();
        acceptedReactions.setPositive(posting.getAcceptedReactionsPositive());
        acceptedReactions.setNegative(posting.getAcceptedReactionsNegative());
        reactions = new ReactionTotalsInfo(posting.getReactionTotals(),
                isAdminOrOwner || posting.isReactionTotalsVisible());
        if (includeSource) {
            reactionsVisible = posting.isReactionsVisible();
            reactionTotalsVisible = posting.isReactionTotalsVisible();
        }
        sources = posting.getSources() != null
                ? posting.getSources().stream().map(PostingSourceInfo::new).collect(Collectors.toList())
                : Collections.emptyList();
    }

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

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public Body getBodyPreview() {
        return bodyPreview;
    }

    public void setBodyPreview(Body bodyPreview) {
        this.bodyPreview = bodyPreview;
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

    public String getBodyFormat() {
        return bodyFormat;
    }

    public void setBodyFormat(String bodyFormat) {
        this.bodyFormat = bodyFormat;
    }

    public String getHeading() {
        return heading;
    }

    public void setHeading(String heading) {
        this.heading = heading;
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

    public Long getDeadline() {
        return deadline;
    }

    public void setDeadline(Long deadline) {
        this.deadline = deadline;
    }

    public Boolean getDraft() {
        return draft;
    }

    public void setDraft(Boolean draft) {
        this.draft = draft;
    }

    public Boolean getDraftPending() {
        return draftPending;
    }

    public void setDraftPending(Boolean draftPending) {
        this.draftPending = draftPending;
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

    public Map<String, String[]> getOperations() {
        return operations;
    }

    public void setOperations(Map<String, String[]> operations) {
        this.operations = operations;
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

    public ReactionTotalsInfo getReactions() {
        return reactions;
    }

    public void setReactions(ReactionTotalsInfo reactions) {
        this.reactions = reactions;
    }

    public Boolean getReactionsVisible() {
        return reactionsVisible;
    }

    public void setReactionsVisible(Boolean reactionsVisible) {
        this.reactionsVisible = reactionsVisible;
    }

    public Boolean getReactionTotalsVisible() {
        return reactionTotalsVisible;
    }

    public void setReactionTotalsVisible(Boolean reactionTotalsVisible) {
        this.reactionTotalsVisible = reactionTotalsVisible;
    }

    public List<PostingSourceInfo> getSources() {
        return sources;
    }

    public void setSources(List<PostingSourceInfo> sources) {
        this.sources = sources;
    }

    public void toPickedPosting(Posting posting) {
        posting.setReceiverEntryId(isOriginal() ? id : receiverPostingId);
        posting.setOwnerName(ownerName);
        posting.setReceiverCreatedAt(Util.toTimestamp(isOriginal() ? createdAt : receiverCreatedAt));
        posting.setAcceptedReactionsPositive(acceptedReactions.getPositive());
        posting.setAcceptedReactionsNegative(acceptedReactions.getNegative());
    }

    public boolean differFromPickedPosting(Posting posting) {
        return posting == null
                || posting.getDeletedAt() != null
                || !posting.getAcceptedReactionsPositive().equals(acceptedReactions.getPositive())
                || !posting.getAcceptedReactionsNegative().equals(acceptedReactions.getNegative())
                || !posting.getCurrentReceiverRevisionId().equals(receiverRevisionId);
    }

}
