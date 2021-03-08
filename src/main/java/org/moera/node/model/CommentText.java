package org.moera.node.model;

import java.util.UUID;
import javax.validation.Valid;
import javax.validation.constraints.Size;

import org.moera.node.data.BodyFormat;
import org.moera.node.data.Entry;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.SourceFormat;
import org.moera.node.text.HeadingExtractor;
import org.moera.node.text.HtmlSanitizer;
import org.moera.node.text.shorten.Shortener;
import org.moera.node.text.TextConverter;
import org.moera.node.util.Util;
import org.springframework.util.StringUtils;

public class CommentText {

    private String ownerName;

    private String ownerFullName;

    private String bodyPreview;

    private String bodySrc;

    private SourceFormat bodySrcFormat;

    private String body;

    @Size(max = 75)
    private String bodyFormat;

    private Long createdAt;

    @Valid
    private AcceptedReactions acceptedReactions;

    private UUID repliedToId;

    private byte[] signature;

    private short signatureVersion;

    public CommentText() {
    }

    public CommentText(String ownerName, String ownerFullName, CommentSourceText sourceText,
                       TextConverter textConverter) {
        this.ownerName = ownerName;
        this.ownerFullName = ownerFullName;
        bodySrc = sourceText.getBodySrc();
        bodySrcFormat = sourceText.getBodySrcFormat() != null ? sourceText.getBodySrcFormat() : SourceFormat.PLAIN_TEXT;
        createdAt = Util.toEpochSecond(Util.now());
        acceptedReactions = sourceText.getAcceptedReactions();
        repliedToId = sourceText.getRepliedToId();
        if (bodySrcFormat != SourceFormat.APPLICATION) {
            Body decodedBody = textConverter.toHtml(bodySrcFormat, new Body(bodySrc));
            body = decodedBody.getEncoded();
            bodyFormat = BodyFormat.MESSAGE.getValue();
            Body decodedBodyPreview = Shortener.shorten(decodedBody);
            if (decodedBodyPreview == null) {
                decodedBodyPreview = new Body(Body.EMPTY);
            }
            bodyPreview = decodedBodyPreview.getEncoded();
        } else {
            body = new Body(bodySrc).getEncoded();
            bodyFormat = BodyFormat.APPLICATION.getValue();
        }
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

    public String getBodyPreview() {
        return bodyPreview;
    }

    public void setBodyPreview(String bodyPreview) {
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

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
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

    public UUID getRepliedToId() {
        return repliedToId;
    }

    public void setRepliedToId(UUID repliedToId) {
        this.repliedToId = repliedToId;
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
        if (ownerFullName != null) {
            entry.setOwnerFullName(ownerFullName);
        }
    }

    public boolean sameAsEntry(Entry entry) {
        return (acceptedReactions == null
                || (acceptedReactions.getPositive() == null
                        || acceptedReactions.getPositive().equals(entry.getAcceptedReactionsPositive()))
                    && (acceptedReactions.getNegative() == null
                        || acceptedReactions.getNegative().equals(entry.getAcceptedReactionsNegative())))
               && (ownerFullName == null || ownerFullName.equals(entry.getOwnerFullName()));
    }

    public void toEntryRevision(EntryRevision revision, byte[] digest, TextConverter textConverter) {
        if (createdAt != null) {
            revision.setCreatedAt(Util.toTimestamp(createdAt));
        }
        if (bodySrcFormat != null) {
            revision.setBodySrcFormat(bodySrcFormat);
        }
        revision.setSignature(signature);
        revision.setSignatureVersion(signatureVersion);
        revision.setDigest(digest);

        Body body = new Body();
        if (signature == null && (this.body == null || StringUtils.isEmpty(this.body))) {
            if (!StringUtils.isEmpty(bodySrc)) {
                if (revision.getBodySrcFormat() != SourceFormat.APPLICATION) {
                    revision.setBodySrc(bodySrc);
                    body = textConverter.toHtml(revision.getBodySrcFormat(), new Body(bodySrc));
                    revision.setBody(body.getEncoded());
                    revision.setSaneBody(HtmlSanitizer.sanitizeIfNeeded(body, false));
                    revision.setBodyFormat(BodyFormat.MESSAGE.getValue());
                    Body bodyPreview = Shortener.shorten(body);
                    if (bodyPreview != null) {
                        revision.setBodyPreview(bodyPreview.getEncoded());
                        revision.setSaneBodyPreview(HtmlSanitizer.sanitizeIfNeeded(bodyPreview, true));
                    } else {
                        revision.setBodyPreview(Body.EMPTY);
                        revision.setSaneBodyPreview(HtmlSanitizer.sanitizeIfNeeded(body, true));
                    }
                } else {
                    revision.setBodySrc(Body.EMPTY);
                    revision.setBody(bodySrc);
                    revision.setSaneBody(null);
                    revision.setBodyFormat(BodyFormat.APPLICATION.getValue());
                }
            }
        } else {
            revision.setBodySrc(bodySrc);
            revision.setBodyFormat(bodyFormat);
            if (BodyFormat.MESSAGE.getValue().equals(bodyFormat)) {
                try {
                    body = new Body(this.body);
                    revision.setBody(this.body);
                    revision.setSaneBody(HtmlSanitizer.sanitizeIfNeeded(body, false));
                } catch (BodyMappingException e) {
                    e.setField("body");
                    throw e;
                }
                try {
                    Body bodyPreview = new Body(this.bodyPreview);
                    revision.setBodyPreview(this.bodyPreview);
                    revision.setSaneBodyPreview(HtmlSanitizer.sanitizeIfNeeded(
                            !StringUtils.isEmpty(bodyPreview.getText()) ? bodyPreview : body, true));
                } catch (BodyMappingException e) {
                    e.setField("bodyPreview");
                    throw e;
                }
            } else {
                revision.setBody(this.body);
                revision.setSaneBody(null);
            }
        }
        if (!revision.getBodyFormat().equals(BodyFormat.APPLICATION.getValue())) {
            revision.setHeading(HeadingExtractor.extract(body));
        }
    }

    public boolean sameAsRevision(EntryRevision revision) {
        return (StringUtils.isEmpty(bodySrcFormat) || bodySrcFormat == revision.getBodySrcFormat())
                && (StringUtils.isEmpty(bodySrc)
                    || (revision.getBodySrcFormat() != SourceFormat.APPLICATION
                        ? bodySrc.equals(revision.getBodySrc()) : bodySrc.equals(revision.getBody())))
                && (revision.getSignature() != null || signature == null);
    }

}
