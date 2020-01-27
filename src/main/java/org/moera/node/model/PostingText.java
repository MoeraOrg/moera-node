package org.moera.node.model;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
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

public class PostingText {

    @NotBlank
    @Size(max = 65535)
    private String bodySrc;

    private String bodySrcFormat;

    private Long publishAt;

    @Valid
    private AcceptedReactions acceptedReactions;

    private Boolean reactionsVisible;

    private Boolean reactionTotalsVisible;

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

    public Long getPublishAt() {
        return publishAt;
    }

    public void setPublishAt(Long publishAt) {
        this.publishAt = publishAt;
    }

    public AcceptedReactions getAcceptedReactions() {
        return acceptedReactions;
    }

    public void setAcceptedReactions(AcceptedReactions acceptedReactions) {
        this.acceptedReactions = acceptedReactions;
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

    public void toEntry(Entry entry) {
        if (acceptedReactions != null) {
            if (acceptedReactions.getPositive() != null) {
                entry.setAcceptedReactionsPositive(acceptedReactions.getPositive());
            }
            if (acceptedReactions.getNegative() != null) {
                entry.setAcceptedReactionsNegative(acceptedReactions.getNegative());
            }
        }
        if (reactionsVisible != null) {
            entry.setReactionsVisible(reactionsVisible);
        }
        if (reactionTotalsVisible != null) {
            entry.setReactionTotalsVisible(reactionTotalsVisible);
        }
    }

    public void toEntryRevision(EntryRevision revision) {
        if (!StringUtils.isEmpty(bodySrcFormat)) {
            SourceFormat format = SourceFormat.forValue(bodySrcFormat);
            if (format == null) {
                throw new ValidationFailure("postingText.bodySrcFormat.unknown");
            }
            revision.setBodySrcFormat(format);
        }

        if (revision.getBodySrcFormat() != SourceFormat.APPLICATION) {
            revision.setBodySrc(bodySrc);
            Body body = TextConverter.toHtml(revision.getBodySrcFormat(), new Body(bodySrc));
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
        if (publishAt != null) {
            revision.setPublishedAt(Util.toTimestamp(publishAt));
        }
    }

}
