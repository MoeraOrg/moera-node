package org.moera.node.text;

import org.moera.node.data.SourceFormat;

public class TextConverter {

    public static String toHtml(SourceFormat format, String source) {
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

}
