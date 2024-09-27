package org.moera.node.text.delta.converter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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
        document.eachLine((line, attributeMap) -> {
            if (isEmptyLine(line)) {
                current.set(null);
            } else {
                Paragraph paragraph = current.get();
                if (paragraph == null) {
                    paragraph = new Paragraph();
                    current.set(paragraph);
                    textSlice.getParagraphs().add(paragraph);
                }
                paragraph.getLines().add(line);
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

    public String toHtml() {
        StringBuilder buf = new StringBuilder();

        paragraphs.forEach(paragraph -> {
            buf.append("<p>");
            buf.append(paragraph.toHtml());
            buf.append("</p>");
        });

        return buf.toString();
    }

}
