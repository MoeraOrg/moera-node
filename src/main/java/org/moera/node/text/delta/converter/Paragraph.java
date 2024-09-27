package org.moera.node.text.delta.converter;

import java.util.ArrayList;
import java.util.List;

import org.moera.node.text.delta.model.Delta;

public class Paragraph {

    private final List<Delta> lines = new ArrayList<>();

    public List<Delta> getLines() {
        return lines;
    }

    public String toHtml() {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i != 0) {
                buf.append("<br>");
            }
            buf.append(LineConverter.toHtml(lines.get(i)));
        }
        return buf.toString();
    }

}
