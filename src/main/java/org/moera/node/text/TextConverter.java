package org.moera.node.text;

import javax.inject.Inject;

import org.moera.node.data.SourceFormat;
import org.moera.node.model.Body;
import org.moera.node.model.BodyMappingException;
import org.springframework.stereotype.Component;

@Component
public class TextConverter {

    @Inject
    private MarkdownConverter markdownConverter;

    private String toHtml(SourceFormat format, String source) {
        switch (format) {
            case PLAIN_TEXT:
                return PlainTextConverter.toHtml(source);
            case HTML:
                return HtmlProcessor.process(source);
            case MARKDOWN:
                return markdownConverter.toHtml(source);
            default:
                throw new IllegalArgumentException("Unknown source format: " + format.name());
        }
    }

    public Body toHtml(SourceFormat format, Body source) {
        Body converted = new Body();
        converted.setSubject(source.getSubject());
        try {
            converted.setText(toHtml(format, source.getText()));
        } catch (Exception e) {
            throw new BodyMappingException();
        }
        return converted;
    }

}
