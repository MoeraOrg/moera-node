package org.moera.node.text.delta.converter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.moera.node.text.delta.model.AttributeMap;
import org.moera.node.text.delta.model.Delta;
import org.moera.node.text.delta.model.OpList;

public class TextSlice {

    private final List<Paragraph> paragraphs = new ArrayList<>();

    public List<Paragraph> getParagraphs() {
        return paragraphs;
    }

    public static TextSlice parse(Delta document) {
        TextSlice textSlice = new TextSlice();

        AtomicReference<Paragraph> current = new AtomicReference<>();
        document.eachLine((line, lineAttributes) -> {
            if (isEmptyLine(line)) {
                current.set(null);
            } else {
                Paragraph paragraph = current.get();
                int quoteLevel = getQuoteLevel(lineAttributes);
                if (paragraph == null
                        || !paragraph.continuesWith(lineAttributes)
                        || paragraph.getQuoteLevel() != quoteLevel) {
                    paragraph = createParagraph(lineAttributes, quoteLevel);
                    current.set(paragraph);
                    textSlice.getParagraphs().add(paragraph);
                }
                paragraph.getLines().add(new Line(line, lineAttributes));
            }
            return true;
        });

        return textSlice;
    }

    private static boolean isEmptyLine(Delta line) {
        OpList ops = line.getOps();
        return ops.isEmpty()
                || ops.size() == 1 && ops.get(0).isTextInsert() && ops.get(0).argAsString().matches("\\s*\n");
    }

    private static int getQuoteLevel(AttributeMap lineAttributes) {
        if (lineAttributes == null) {
            return 0;
        }
        if (lineAttributes.containsKey("quote-level")) {
            return Integer.parseInt((String) lineAttributes.get("quote-level"));
        }
        if (lineAttributes.containsKey("blockquote")) {
            return 1;
        }
        return 0;
    }

    private static Paragraph createParagraph(AttributeMap lineAttributes, int quoteLevel) {
        if (lineAttributes == null) {
            return new Paragraph(quoteLevel);
        }
        if (lineAttributes.containsKey("header")) {
            return new Header((Integer) lineAttributes.get("header"), quoteLevel);
        }
        if (lineAttributes.containsKey("list")) {
            return new MarkedList(quoteLevel);
        }
        return new Paragraph(quoteLevel);
    }

    public String toHtml() {
        StringBuilder buf = new StringBuilder();
        AtomicInteger quoteLevel = new AtomicInteger(0);
        paragraphs.forEach(paragraph -> {
            buf.append("</blockquote>".repeat(Math.max(0, quoteLevel.get() - paragraph.getQuoteLevel())));
            buf.append("<blockquote>".repeat(Math.max(0, paragraph.getQuoteLevel() - quoteLevel.get())));
            buf.append(paragraph.toHtml());
            quoteLevel.set(paragraph.getQuoteLevel());
        });
        buf.append("</blockquote>".repeat(quoteLevel.get()));
        return buf.toString();
    }

}
