package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.commons.crypto.CryptoUtil;
import org.moera.node.data.EntryRevision;
import org.moera.node.util.Util;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostingRevisionInfo {

    private String id;
    private Body bodyPreview;
    private byte[] bodySrcHash;
    private String bodySrcFormat;
    private Body body;
    private String bodyFormat;
    private String heading;
    private long createdAt;
    private long publishedAt;
    private Long deletedAt;
    private boolean pinned;
    private byte[] signature;
    private short signatureVersion;
    private ClientReactionInfo clientReaction;
    private ReactionTotalsInfo reactions;

    public PostingRevisionInfo() {
    }

    public PostingRevisionInfo(EntryRevision revision, boolean countsVisible) {
        id = revision.getId().toString();
        bodyPreview = new Body(revision.getBodyPreview());
        bodySrcHash = CryptoUtil.digest(revision.getBodySrc());
        bodySrcFormat = revision.getBodySrcFormat().getValue();
        body = new Body(revision.getBody());
        bodyFormat = revision.getBodyFormat();
        heading = revision.getHeading();
        createdAt = Util.toEpochSecond(revision.getCreatedAt());
        publishedAt = Util.toEpochSecond(revision.getPublishedAt());
        deletedAt = Util.toEpochSecond(revision.getDeletedAt());
        pinned = revision.isPinned();
        signature = revision.getSignature();
        signatureVersion = revision.getSignatureVersion();
        reactions = new ReactionTotalsInfo(revision.getReactionTotals(), countsVisible);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Body getBodyPreview() {
        return bodyPreview;
    }

    public void setBodyPreview(Body bodyPreview) {
        this.bodyPreview = bodyPreview;
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

    public long getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(long publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Long getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Long deletedAt) {
        this.deletedAt = deletedAt;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
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
