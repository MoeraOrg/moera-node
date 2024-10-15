package org.moera.node.text.delta.converter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Paragraph extends Block {

    protected final List<Line> lines = new ArrayList<>();

    public Paragraph(int quoteLevel) {
        super(quoteLevel);
    }

    public List<Line> getLines() {
        return lines;
    }

    @Override
    public void addLine(Line line) {
        lines.add(line);
    }

    @Override
    public boolean continuesWith(Map<String, Object> lineAttributes) {
        return lineAttributes == null
                || !lineAttributes.containsKey("header")
                    && !lineAttributes.containsKey("list")
                    && !lineAttributes.containsKey("horizontal-rule");
    }

    @Override
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
