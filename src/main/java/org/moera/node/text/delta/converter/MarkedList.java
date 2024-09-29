package org.moera.node.text.delta.converter;

import java.util.ArrayDeque;
import java.util.Deque;

import org.moera.node.text.delta.model.AttributeMap;

public class MarkedList extends Paragraph {

    @Override
    public boolean continuesWith(AttributeMap lineAttributes) {
        return lineAttributes != null && lineAttributes.containsKey("list");
    }

    @Override
    public String toHtml() {
        StringBuilder buf = new StringBuilder();

        Deque<String> opened = new ArrayDeque<>();
        for (Line line : lines) {
            int indent = (Integer) line.getAttributes().getOrDefault("indent", 0) + 1;
            if (opened.size() > indent) {
                for (int i = opened.size(); i > indent; i--) {
                    closeList(opened, buf);
                }
                buf.append("</li><li>");
            } else if (opened.size() < indent) {
                for (int i = opened.size(); i < indent; i++) {
                    String tag = listTypeToTag((String) line.getAttributes().get("list"));
                    openList(buf, tag, opened);
                }
            } else {
                String tag = listTypeToTag((String) line.getAttributes().get("list"));
                if (tag.equals(opened.peek())) {
                    buf.append("</li><li>");
                } else {
                    closeList(opened, buf);
                    openList(buf, tag, opened);
                }
            }
            buf.append(LineConverter.toHtml(line.getDelta()));
        }
        while (!opened.isEmpty()) {
            closeList(opened, buf);
        }

        return buf.toString();
    }

    private void openList(StringBuilder buf, String tag, Deque<String> opened) {
        buf.append('<');
        buf.append(tag);
        if (tag.equals("ol")) {
            buf.append(" type=\"");
            buf.append(numberingType(opened));
            buf.append("\"");
        }
        buf.append('>');
        buf.append("<li>");
        opened.push(tag);
    }

    private void closeList(Deque<String> opened, StringBuilder buf) {
        String tag = opened.pop();
        buf.append("</li>");
        buf.append("</");
        buf.append(tag);
        buf.append('>');
    }

    private String listTypeToTag(String type) {
        return switch (type) {
            case "ordered" -> "ol";
            case "bullet" -> "ul";
            default -> "ul";
        };
    }

    private String numberingType(Deque<String> opened) {
        int level = (int) opened.stream().filter(t -> t.equals("ol")).count();
        return switch (level % 3) {
            case 0 -> "1";
            case 1 -> "a";
            case 2 -> "i";
            default -> throw new IllegalStateException("Mathematically impossible value: " + level % 3);
        };
    }

}
