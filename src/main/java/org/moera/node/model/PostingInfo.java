package org.moera.node.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.commons.crypto.CryptoUtil;
import org.moera.node.data.Posting;
import org.moera.node.option.Options;
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
    private byte[] signature;
    private short signatureVersion;
    private long moment;
    private Map<String, String[]> operations;
    private String acceptedReactionsPositive;
    private String acceptedReactionsNegative;
    private ClientReactionInfo clientReaction;
    private ReactionTotalsInfo reactions;

    public PostingInfo() {
    }

    public PostingInfo(Posting posting, Options options) {
        this(posting, false, options);
    }

    public PostingInfo(Posting posting, boolean includeSource, Options options) {
        id = posting.getId().toString();
        revisionId = posting.getCurrentRevision().getId().toString();
        totalRevisions = posting.getTotalRevisions();
        receiverName = posting.getReceiverName();
        ownerName = posting.getOwnerName();
        bodyPreview = new Body(posting.getCurrentRevision().getBodyPreview());
        if (includeSource) {
            bodySrc = new Body(posting.getCurrentRevision().getBodySrc());
        }
        bodySrcHash = CryptoUtil.digest(posting.getCurrentRevision().getBodySrc());
        bodySrcFormat = posting.getCurrentRevision().getBodySrcFormat().getValue();
        body = new Body(posting.getCurrentRevision().getBody());
        bodyFormat = posting.getCurrentRevision().getBodyFormat();
        heading = posting.getCurrentRevision().getHeading();
        createdAt = Util.toEpochSecond(posting.getCreatedAt());
        editedAt = Util.toEpochSecond(posting.getCurrentRevision().getCreatedAt());
        deletedAt = Util.toEpochSecond(posting.getDeletedAt());
        publishedAt = Util.toEpochSecond(posting.getCurrentRevision().getPublishedAt());
        signature = posting.getCurrentRevision().getSignature();
        signatureVersion = posting.getCurrentRevision().getSignatureVersion();
        moment = posting.getCurrentRevision().getMoment();
        operations = new HashMap<>();
        operations.put("edit", new String[]{"owner"});
        operations.put("delete", new String[]{"owner", "admin"});
        operations.put("revisions", new String[0]);
        acceptedReactionsPositive = posting.getAcceptedReactionsPositiveOrDefault(options);
        acceptedReactionsNegative = posting.getAcceptedReactionsNegativeOrDefault(options);
        reactions = new ReactionTotalsInfo(posting.getReactionTotals());
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

    public long getMoment() {
        return moment;
    }

    public void setMoment(long moment) {
        this.moment = moment;
    }

    public Map<String, String[]> getOperations() {
        return operations;
    }

    public void setOperations(Map<String, String[]> operations) {
        this.operations = operations;
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

}
