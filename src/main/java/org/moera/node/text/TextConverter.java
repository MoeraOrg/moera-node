package org.moera.node.text;

import org.moera.node.data.SourceFormat;
import org.moera.node.model.Body;

public class TextConverter {

    private static String toHtml(SourceFormat format, String source) {
        switch (format) {
            case PLAIN_TEXT:
                return PlainTextConverter.toHtml(source);
            case HTML:
                return source;
            case MARKDOWN:
                return MarkdownConverter.toHtml(source);
            default:
                throw new IllegalArgumentException("Unknown source format: " + format.name());
        }
    }

    public static Body toHtml(SourceFormat format, Body source) {
        Body converted = new Body();
        converted.setSubject(source.getSubject());
        converted.setText(toHtml(format, source.getText()));
        return converted;
    }

}
