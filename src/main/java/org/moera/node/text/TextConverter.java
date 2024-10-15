package org.moera.node.text;

import java.util.List;
import javax.inject.Inject;

import org.moera.node.data.BodyFormat;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.SourceFormat;
import org.moera.node.model.body.Body;
import org.moera.node.model.body.BodyMappingException;
import org.moera.node.text.delta.converter.DeltaConverter;
import org.moera.node.text.markdown.MarkdownConverter;
import org.moera.node.text.sanitizer.HtmlSanitizer;
import org.moera.node.text.shorten.Shortener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class TextConverter {

    private static final Logger log = LoggerFactory.getLogger(TextConverter.class);

    @Inject
    private MarkdownConverter markdownConverter;

    @Inject
    private DeltaConverter deltaConverter;

    public String toHtml(SourceFormat format, String source) {
        return switch (format) {
            case PLAIN_TEXT -> PlainTextConverter.toHtml(source);
            case HTML -> source;
            case MARKDOWN -> markdownConverter.toHtml(source);
            case DELTA -> deltaConverter.toHtml(source);
            default -> throw new IllegalArgumentException("Unknown source format: " + format.name());
        };
    }

    public Body toHtml(SourceFormat format, Body source) {
        Body converted = source.clone();
        converted.setSubject(source.getSubject());
        try {
            converted.setText(toHtml(format, source.getText()));
        } catch (Exception e) {
            log.warn("Text conversion error", e);
            throw new BodyMappingException();
        }
        return converted;
    }

    public void toRevision(String bodySrc, String sourceBody, String bodyFormat, String sourceBodyPreview,
                           boolean isSigned, List<MediaFileOwner> media, boolean collapseQuotations,
                           EntryRevision revision) {
        Body body = new Body();
        if (!isSigned && ObjectUtils.isEmpty(sourceBody)) {
            if (!ObjectUtils.isEmpty(bodySrc)) {
                if (revision.getBodySrcFormat() != SourceFormat.APPLICATION) {
                    revision.setBodySrc(bodySrc);
                    body = toHtml(revision.getBodySrcFormat(), new Body(bodySrc));
                    revision.setBody(body.getEncoded());
                    revision.setSaneBody(HtmlSanitizer.sanitizeIfNeeded(body, false, media));
                    revision.setBodyFormat(BodyFormat.MESSAGE.getValue());
                    Body bodyPreview = Shortener.shorten(body, hasAttachedGallery(body, media));
                    if (bodyPreview != null) {
                        revision.setBodyPreview(bodyPreview.getEncoded());
                        revision.setSaneBodyPreview(HtmlSanitizer.sanitizeIfNeeded(bodyPreview, true, media));
                    } else {
                        revision.setBodyPreview(Body.EMPTY);
                        revision.setSaneBodyPreview(HtmlSanitizer.sanitizeIfNeeded(body, true, media));
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
                    body = new Body(sourceBody);
                    revision.setBody(sourceBody);
                    revision.setSaneBody(HtmlSanitizer.sanitizeIfNeeded(body, false, media));
                } catch (BodyMappingException e) {
                    e.setField("body");
                    throw e;
                }
                try {
                    Body bodyPreview = new Body(sourceBodyPreview);
                    revision.setBodyPreview(sourceBodyPreview);
                    revision.setSaneBodyPreview(HtmlSanitizer.sanitizeIfNeeded(
                            !ObjectUtils.isEmpty(bodyPreview.getText()) ? bodyPreview : body, true, media));
                } catch (BodyMappingException e) {
                    e.setField("bodyPreview");
                    throw e;
                }
            } else {
                revision.setBody(sourceBody);
                revision.setSaneBody(null);
            }
        }
        if (!revision.getBodyFormat().equals(BodyFormat.APPLICATION.getValue())) {
            String heading = HeadingExtractor.extractHeading(body, hasAttachedGallery(body, media), collapseQuotations);
            revision.setHeading(heading);
            revision.setDescription(HeadingExtractor.extractDescription(body, collapseQuotations, heading));
        }
    }

    private static boolean hasAttachedGallery(Body body, List<MediaFileOwner> media) {
        if (ObjectUtils.isEmpty(media)) {
            return false;
        }
        int embeddedCount = MediaExtractor.extractMediaFileIds(body).size();
        return media.size() > embeddedCount;
    }

}
