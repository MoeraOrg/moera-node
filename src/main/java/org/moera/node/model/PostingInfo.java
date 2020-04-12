package org.moera.node.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.commons.crypto.CryptoUtil;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.Feed;
import org.moera.node.data.Posting;
import org.moera.node.data.Story;
import org.moera.node.util.Util;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostingInfo {

    private String id;
    private String revisionId;
    private int totalRevisions;
    private String receiverName;
    private String ownerName;
    private Body bodyPreview;
    private Body bodySrc;
    private byte[] bodySrcHash;
    private String bodySrcFormat;
    private Body body;
    private String bodyFormat;
    private String heading;
    private long createdAt;
    private long editedAt;
    private Long deletedAt;
    private Long deadline;
    private boolean draft;
    private Boolean draftPending;
    private byte[] signature;
    private short signatureVersion;
    private List<FeedReference> feedReferences;
    private Map<String, String[]> operations;
    private AcceptedReactions acceptedReactions = new AcceptedReactions();
    private ClientReactionInfo clientReaction;
    private ReactionTotalsInfo reactions;
    private Boolean reactionsVisible;
    private Boolean reactionTotalsVisible;

    public PostingInfo() {
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
        totalRevisions = posting.getTotalRevisions();
        receiverName = posting.getReceiverName();
        ownerName = posting.getOwnerName();
        bodyPreview = new Body(revision.getBodyPreview());
        if (includeSource) {
            bodySrc = new Body(revision.getBodySrc());
        }
        bodySrcHash = CryptoUtil.digest(revision.getBodySrc());
        bodySrcFormat = revision.getBodySrcFormat().getValue();
        body = new Body(revision.getBody());
        bodyFormat = revision.getBodyFormat();
        heading = revision.getHeading();
        createdAt = Util.toEpochSecond(posting.getCreatedAt());
        editedAt = Util.toEpochSecond(revision.getCreatedAt());
        deletedAt = Util.toEpochSecond(posting.getDeletedAt());
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
        operations.put("edit", new String[]{"owner"});
        operations.put("delete", new String[]{"owner", "admin"});
        operations.put("revisions", new String[0]);
        operations.put("reactions",
                posting.isReactionsVisible() ? new String[]{"public"} : new String[]{"owner", "admin"});
        acceptedReactions.setPositive(posting.getAcceptedReactionsPositive());
        acceptedReactions.setNegative(posting.getAcceptedReactionsNegative());
        reactions = new ReactionTotalsInfo(posting.getReactionTotals(),
                isAdminOrOwner || posting.isReactionTotalsVisible());
        if (includeSource) {
            reactionsVisible = posting.isReactionsVisible();
            reactionTotalsVisible = posting.isReactionTotalsVisible();
        }
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

    public int getTotalRevisions() {
        return totalRevisions;
    }

    public void setTotalRevisions(int totalRevisions) {
        this.totalRevisions = totalRevisions;
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

    public String getBodySrcFormat() {
        return bodySrcFormat;
    }

    public void setBodySrcFormat(String bodySrcFormat) {
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

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getEditedAt() {
        return editedAt;
    }

    public void setEditedAt(long editedAt) {
        this.editedAt = editedAt;
    }

    public Long getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Long deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Long getDeadline() {
        return deadline;
    }

    public void setDeadline(Long deadline) {
        this.deadline = deadline;
    }

    public boolean isDraft() {
        return draft;
    }

    public void setDraft(boolean draft) {
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

    public short getSignatureVersion() {
        return signatureVersion;
    }

    public void setSignatureVersion(short signatureVersion) {
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

}
