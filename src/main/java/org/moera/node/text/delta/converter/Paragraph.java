package org.moera.node.text.delta.converter;

import java.util.ArrayList;
import java.util.List;

import org.moera.node.text.delta.model.AttributeMap;
import org.moera.node.text.delta.model.Delta;

public class Paragraph {

    private final List<Delta> lines = new ArrayList<>();

    public List<Delta> getLines() {
        return lines;
    }

    public boolean continuesWith(AttributeMap lineAttributes) {
        return lineAttributes == null || !lineAttributes.containsKey("header");
    }

    public String toHtml() {
        return toHtml("p");
    }

    protected String toHtml(String tagName) {
        StringBuilder buf = new StringBuilder();
        buf.append('<');
        buf.append(tagName);
        buf.append('>');
        for (int i = 0; i < lines.size(); i++) {
            if (i != 0) {
                buf.append("<br>");
            }
            buf.append(LineConverter.toHtml(lines.get(i)));
        }
        buf.append("</");
        buf.append(tagName);
        buf.append('>');
        return buf.toString();
    }

}
