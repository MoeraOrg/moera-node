package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.lib.crypto.CryptoUtil;
import org.moera.lib.node.types.ClientReactionInfo;
import org.moera.lib.node.types.MediaAttachment;
import org.moera.lib.node.types.ReactionTotalsInfo;
import org.moera.lib.node.types.SourceFormat;
import org.moera.lib.node.types.UpdateInfo;
import org.moera.lib.node.types.body.Body;
import org.moera.lib.node.types.principal.AccessChecker;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.Posting;
import org.moera.node.operations.MediaAttachmentsProvider;
import org.moera.node.util.Util;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostingRevisionInfo implements RevisionInfo {

    private String id;
    private String receiverId;
    private Body bodyPreview;
    private byte[] bodySrcHash;
    private SourceFormat bodySrcFormat;
    private Body body;
    private String bodyFormat;
    private MediaAttachment[] media;
    private String heading;
    private UpdateInfo updateInfo;
    private Long createdAt;
    private Long deletedAt;
    private Long receiverCreatedAt;
    private Long receiverDeletedAt;
    private byte[] digest;
    private byte[] signature;
    private short signatureVersion;
    private ClientReactionInfo clientReaction;
    private ReactionTotalsInfo reactions;

    public PostingRevisionInfo() {
    }

    public PostingRevisionInfo(Posting posting, EntryRevision revision,
                               MediaAttachmentsProvider mediaAttachmentsProvider, String receiverName,
                               AccessChecker accessChecker) {
        id = revision.getId().toString();
        receiverId = revision.getReceiverRevisionId();
        bodyPreview = new Body(revision.getBodyPreview());
        bodySrcHash = revision.getReceiverBodySrcHash() != null
                ? revision.getReceiverBodySrcHash()
                : CryptoUtil.digest(CryptoUtil.fingerprint(revision.getBodySrc()));
        bodySrcFormat = revision.getBodySrcFormat();
        body = new Body(revision.getBody());
        bodyFormat = revision.getBodyFormat();
        media = mediaAttachmentsProvider.getMediaAttachments(revision, receiverName);
        heading = revision.getHeading();
        if (!UpdateInfoUtil.isEmpty(revision)) {
            updateInfo = UpdateInfoUtil.build(revision);
        }
        createdAt = Util.toEpochSecond(revision.getCreatedAt());
        deletedAt = Util.toEpochSecond(revision.getDeletedAt());
        receiverCreatedAt = Util.toEpochSecond(revision.getReceiverCreatedAt());
        receiverDeletedAt = Util.toEpochSecond(revision.getReceiverDeletedAt());
        digest = revision.getDigest();
        signature = revision.getSignature();
        signatureVersion = revision.getSignatureVersion();
        reactions = ReactionTotalsInfoUtil.build(revision.getReactionTotals(), posting, accessChecker);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
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

    @Override
    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    @Override
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

    public Long getReceiverDeletedAt() {
        return receiverDeletedAt;
    }

    public void setReceiverDeletedAt(Long receiverDeletedAt) {
        this.receiverDeletedAt = receiverDeletedAt;
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
