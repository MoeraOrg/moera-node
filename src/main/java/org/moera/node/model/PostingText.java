package org.moera.node.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import org.moera.node.data.Posting;
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
    private String bodyHtml;

    private Long publishAt;

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

    public String getBodyHtml() {
        return bodyHtml;
    }

    public void setBodyHtml(String bodyHtml) {
        this.bodyHtml = bodyHtml;
    }

    public Long getPublishAt() {
        return publishAt;
    }

    public void setPublishAt(Long publishAt) {
        this.publishAt = publishAt;
    }

    public void toPosting(Posting posting) {
        posting.setBodySrc(bodySrc);
        if (!StringUtils.isEmpty(bodySrcFormat)) {
            SourceFormat format = SourceFormat.forValue(bodySrcFormat);
            if (format == null) {
                throw new ValidationFailure("postingText.bodySrcFormat.unknown");
            }
            posting.setBodySrcFormat(format);
        }

        if (StringUtils.isEmpty(bodyHtml)) {
            bodyHtml = TextConverter.toHtml(posting.getBodySrcFormat(), bodySrc);
        }
        posting.setBodyHtml(bodyHtml);
        if (!Shortener.isShort(bodyHtml)) {
            posting.setBodyPreviewHtml(Shortener.shorten(bodyHtml));
        }
        posting.setHeading(HeadingExtractor.extract(bodyHtml));
        if (publishAt != null) {
            posting.setPublishedAt(Util.toTimestamp(publishAt));
        }
    }

}
