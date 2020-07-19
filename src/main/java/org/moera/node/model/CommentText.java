package org.moera.node.model;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import org.moera.node.data.BodyFormat;
import org.moera.node.data.Entry;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.SourceFormat;
import org.moera.node.text.HeadingExtractor;
import org.moera.node.text.Shortener;
import org.moera.node.text.TextConverter;
import org.moera.node.util.Util;
import org.springframework.util.StringUtils;

public class CommentText {

    private String ownerName;

    private Body bodyPreview;

    @Size(max = 65535)
    private String bodySrc;

    private SourceFormat bodySrcFormat;

    private Body body;

    @Size(max = 75)
    private String bodyFormat;

    private Long createdAt;

    @Valid
    private AcceptedReactions acceptedReactions;

    private byte[] signature;

    private short signatureVersion;

    public CommentText() {
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

    public String getBodySrc() {
        return bodySrc;
    }

    public void setBodySrc(String bodySrc) {
        this.bodySrc = bodySrc;
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

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public AcceptedReactions getAcceptedReactions() {
        return acceptedReactions;
    }

    public void setAcceptedReactions(AcceptedReactions acceptedReactions) {
        this.acceptedReactions = acceptedReactions;
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

    public void toEntry(Entry entry) {
        if (sameAsEntry(entry)) {
            return;
        }

        entry.setEditedAt(Util.now());
        if (acceptedReactions != null) {
            if (acceptedReactions.getPositive() != null) {
                entry.setAcceptedReactionsPositive(acceptedReactions.getPositive());
            }
            if (acceptedReactions.getNegative() != null) {
                entry.setAcceptedReactionsNegative(acceptedReactions.getNegative());
            }
        }
    }

    public boolean sameAsEntry(Entry entry) {
        return acceptedReactions == null
                || (acceptedReactions.getPositive() == null
                        || acceptedReactions.getPositive().equals(entry.getAcceptedReactionsPositive()))
                    && (acceptedReactions.getNegative() == null
                        || acceptedReactions.getNegative().equals(entry.getAcceptedReactionsNegative()));
    }

    public void toEntryRevision(EntryRevision revision, byte[] digest, TextConverter textConverter) {
        if (bodySrcFormat != null) {
            revision.setBodySrcFormat(bodySrcFormat);
        }
        revision.setSignature(signature);
        revision.setSignatureVersion(signatureVersion);
        revision.setDigest(digest);

        if (signature == null
                && (body == null || StringUtils.isEmpty(body.getEncoded()))
                && !StringUtils.isEmpty(bodySrc)) {
            if (revision.getBodySrcFormat() != SourceFormat.APPLICATION) {
                revision.setBodySrc(bodySrc);
                Body body = textConverter.toHtml(revision.getBodySrcFormat(), new Body(bodySrc));
                revision.setBody(body.getEncoded());
                revision.setBodyFormat(BodyFormat.MESSAGE.getValue());
                if (!Shortener.isShort(body)) {
                    revision.setBodyPreview(Shortener.shorten(body).getEncoded());
                } else {
                    revision.setBodyPreview(Body.EMPTY);
                }
                revision.setHeading(HeadingExtractor.extract(body));
            } else {
                revision.setBodySrc(Body.EMPTY);
                revision.setBody(bodySrc);
                revision.setBodyFormat(BodyFormat.APPLICATION.getValue());
            }
        }
    }

    public boolean sameAsRevision(EntryRevision revision) {
        return (StringUtils.isEmpty(bodySrcFormat) || bodySrcFormat == revision.getBodySrcFormat())
                && (StringUtils.isEmpty(bodySrc)
                    || (revision.getBodySrcFormat() != SourceFormat.APPLICATION
                        ? bodySrc.equals(revision.getBodySrc()) : bodySrc.equals(revision.getBody())));
    }

}
