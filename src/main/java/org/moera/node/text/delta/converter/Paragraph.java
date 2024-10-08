package org.moera.node.text.delta.converter;

import java.util.ArrayList;
import java.util.List;

import org.moera.node.text.delta.model.AttributeMap;

public class Paragraph {

    protected final List<Line> lines = new ArrayList<>();
    private final int quoteLevel;

    public Paragraph(int quoteLevel) {
        this.quoteLevel = quoteLevel;
    }

    public List<Line> getLines() {
        return lines;
    }

    public int getQuoteLevel() {
        return quoteLevel;
    }

    public boolean continuesWith(AttributeMap lineAttributes) {
        return lineAttributes == null || !lineAttributes.containsKey("header") && !lineAttributes.containsKey("list");
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
            buf.append(LineConverter.toHtml(lines.get(i).getDelta()));
        }
        buf.append("</");
        buf.append(tagName);
        buf.append('>');
        return buf.toString();
    }

}
