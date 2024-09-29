package org.moera.node.text.delta.converter;

import java.util.ArrayList;
import java.util.List;
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
                if (paragraph == null || !paragraph.continuesWith(lineAttributes)) {
                    paragraph = createParagraph(lineAttributes);
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

    private static Paragraph createParagraph(AttributeMap lineAttributes) {
        if (lineAttributes == null) {
            return new Paragraph();
        }
        if (lineAttributes.containsKey("header")) {
            return new Header((Integer) lineAttributes.get("header"));
        }
        if (lineAttributes.containsKey("list")) {
            return new MarkedList();
        }
        return new Paragraph();
    }

    public String toHtml() {
        StringBuilder buf = new StringBuilder();
        paragraphs.forEach(paragraph -> buf.append(paragraph.toHtml()));
        return buf.toString();
    }

}
