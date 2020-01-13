package org.moera.node.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import org.moera.node.data.Entry;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.SourceFormat;
import org.moera.node.text.HeadingExtractor;
import org.moera.node.text.Shortener;
import org.moera.node.text.TextConverter;
import org.moera.node.util.Util;
import org.springframework.util.StringUtils;

public class PostingText {

    @NotBlank
    @Size(max = 65535)
    private String bodySrc;

    private String bodySrcFormat;

    @Size(max = 65535)
    private String body;

    private String bodyFormat;

    private Long publishAt;

    @Size(max = 255)
    private String acceptedReactionsPositive;

    @Size(max = 255)
    private String acceptedReactionsNegative;

    public PostingText() {
    }

    public String getBodySrc() {
        return bodySrc;
    }

    public void setBodySrc(String bodySrc) {
        this.bodySrc = bodySrc;
    }

    public String getBodySrcFormat() {
        return bodySrcFormat;
    }

    public void setBodySrcFormat(String bodySrcFormat) {
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

    public Long getPublishAt() {
        return publishAt;
    }

    public void setPublishAt(Long publishAt) {
        this.publishAt = publishAt;
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

    public void toEntry(Entry entry) {
        entry.setAcceptedReactionsPositive(acceptedReactionsPositive);
        entry.setAcceptedReactionsNegative(acceptedReactionsNegative);
    }

    public void toEntryRevision(EntryRevision revision) {
        revision.setBodySrc(bodySrc);
        if (!StringUtils.isEmpty(bodySrcFormat)) {
            SourceFormat format = SourceFormat.forValue(bodySrcFormat);
            if (format == null) {
                throw new ValidationFailure("postingText.bodySrcFormat.unknown");
            }
            revision.setBodySrcFormat(format);
        }

        if (StringUtils.isEmpty(body)) {
            body = TextConverter.toHtml(revision.getBodySrcFormat(), new Body(bodySrc)).getEncoded();
            bodyFormat = "html";
        } else {
            if (StringUtils.isEmpty(bodyFormat)) {
                bodyFormat = "html";
            }
        }
        revision.setBody(body);
        revision.setBodyFormat(bodyFormat);
        if (bodyFormat.equals("html")) {
            Body decodedBody = new Body(body);
            if (!Shortener.isShort(decodedBody)) {
                revision.setBodyPreview(Shortener.shorten(decodedBody).getEncoded());
            } else {
                revision.setBodyPreview(Body.EMPTY);
            }
            revision.setHeading(HeadingExtractor.extract(decodedBody));
        }
        if (publishAt != null) {
            revision.setPublishedAt(Util.toTimestamp(publishAt));
        }
    }

}
