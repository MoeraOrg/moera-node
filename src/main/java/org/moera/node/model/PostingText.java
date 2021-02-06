package org.moera.node.model;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.Size;

import org.moera.node.data.BodyFormat;
import org.moera.node.data.Entry;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.SourceFormat;
import org.moera.node.text.HeadingExtractor;
import org.moera.node.text.HtmlSanitizer;
import org.moera.node.text.Shortener;
import org.moera.node.text.TextConverter;
import org.moera.node.util.Util;
import org.springframework.util.StringUtils;

public class PostingText {

    @Size(max = 96)
    private String ownerFullName;

    @Size(max = 256 * 1024 * 1024)
    private String bodySrc;

    private SourceFormat bodySrcFormat;

    @Valid
    private AcceptedReactions acceptedReactions;

    private Boolean reactionsVisible;

    private Boolean reactionTotalsVisible;

    private List<StoryAttributes> publications;

    public PostingText() {
    }

    public String getOwnerFullName() {
        return ownerFullName;
    }

    public void setOwnerFullName(String ownerFullName) {
        this.ownerFullName = ownerFullName;
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

    public List<StoryAttributes> getPublications() {
        return publications;
    }

    public void setPublications(List<StoryAttributes> publications) {
        this.publications = publications;
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
        if (reactionsVisible != null) {
            entry.setReactionsVisible(reactionsVisible);
        }
        if (reactionTotalsVisible != null) {
            entry.setReactionTotalsVisible(reactionTotalsVisible);
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
                && (reactionsVisible == null || reactionsVisible.equals(entry.isReactionsVisible()))
                && (reactionTotalsVisible == null || reactionTotalsVisible.equals(entry.isReactionTotalsVisible()))
                && (ownerFullName == null || ownerFullName.equals(entry.getOwnerFullName()));
    }

    public void toEntryRevision(EntryRevision revision, TextConverter textConverter) {
        if (bodySrcFormat != null) {
            revision.setBodySrcFormat(bodySrcFormat);
        }

        if (!StringUtils.isEmpty(bodySrc)) {
            if (revision.getBodySrcFormat() != SourceFormat.APPLICATION) {
                revision.setBodySrc(bodySrc);
                Body body = textConverter.toHtml(revision.getBodySrcFormat(), new Body(bodySrc));
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
