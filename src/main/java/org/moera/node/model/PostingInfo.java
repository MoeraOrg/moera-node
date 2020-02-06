package org.moera.node.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.commons.crypto.CryptoUtil;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.Posting;
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
    private long publishedAt;
    private Long deadline;
    private boolean pinned;
    private boolean draft;
    private byte[] signature;
    private short signatureVersion;
    private Long moment;
    private Map<String, String[]> operations;
    private AcceptedReactions acceptedReactions = new AcceptedReactions();
    private ClientReactionInfo clientReaction;
    private ReactionTotalsInfo reactions;
    private Boolean reactionsVisible;
    private Boolean reactionTotalsVisible;

    public PostingInfo() {
    }

    public PostingInfo(Posting posting, boolean isAdminOrOwner) {
        this(posting, false, isAdminOrOwner);
    }

    public PostingInfo(Posting posting, boolean includeSource, boolean isAdminOrOwner) {
        this(posting, posting.getCurrentRevision(), includeSource, isAdminOrOwner);
    }

    public PostingInfo(Posting posting, EntryRevision revision, boolean includeSource, boolean isAdminOrOwner) {
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
        publishedAt = Util.toEpochSecond(revision.getPublishedAt());
        deadline = Util.toEpochSecond(posting.getDeadline());
        pinned = revision.isPinned();
        if (posting.isDraft()) {
            draft = true;
        }
        signature = revision.getSignature();
        signatureVersion = revision.getSignatureVersion();
        moment = revision.getMoment();
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

    public long getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(long publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Long getDeadline() {
        return deadline;
    }

    public void setDeadline(Long deadline) {
        this.deadline = deadline;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public boolean isDraft() {
        return draft;
    }

    public void setDraft(boolean draft) {
        this.draft = draft;
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

    public Long getMoment() {
        return moment;
    }

    public void setMoment(Long moment) {
        this.moment = moment;
    }

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
