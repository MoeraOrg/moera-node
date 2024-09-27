package org.moera.node.text.delta.converter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.moera.node.text.delta.model.AttributeMap;
import org.moera.node.text.delta.model.Delta;
import org.moera.node.text.delta.model.Op;
import org.moera.node.util.Util;

public class LineConverter {

    private static final Set<String> KNOWN_FORMATS = Set.of("bold", "code", "italic", "strike", "underline");

    public static String toHtml(Delta line) {
        StringBuilder buf = new StringBuilder();

        String openLink = null;
        Set<String> openFormats = new HashSet<>();
        Deque<String> openStack = new ArrayDeque<>();
        Set<String> toClose = new HashSet<>();
        List<String> toOpen = new ArrayList<>();

        for (Op op : line.getOps()) {
            if (!op.isInsert()) {
                continue;
            }

            AttributeMap attrs = op.attributes();
            if (attrs == null) {
                attrs = new AttributeMap();
            }

            toClose.clear();
            for (String format : openFormats) {
                if (!attrs.containsKey(format)) {
                    toClose.add(format);
                }
            }

            toOpen.clear();
            for (String format : attrs.keySet()) {
                if (KNOWN_FORMATS.contains(format) && !openFormats.contains(format)) {
                    toOpen.add(format);
                }
            }

            String newLink = (String) attrs.get("link");

            while (!openStack.isEmpty()) {
                if (toClose.isEmpty() && Objects.equals(openLink, newLink)) {
                    break;
                }

                String format = openStack.pop();
                openFormats.remove(format);
                if (toClose.contains(format)) {
                    toClose.remove(format);
                    closeTag(format, buf);
                } else {
                    toOpen.add(format);
                }
            }

            if (!Objects.equals(openLink, newLink)) {
                if (openLink != null) {
                    closeLink(buf);
                }
                if (newLink != null) {
                    openLink(newLink, buf);
                    openLink = newLink;
                }
            }

            for (int i = toOpen.size() - 1; i >= 0; i--) {
                String format = toOpen.get(i);
                openTag(format, buf);
                openStack.push(format);
                openFormats.add(format);
            }

            buf.append(op.argAsString());
        }

        return buf.toString();
    }

    private static String formatToTag(String format) {
        return switch (format) {
            case "bold" -> "b";
            case "code" -> "code";
            case "italic" -> "i";
            case "strike" -> "s";
            case "underline" -> "u";
            default -> null;
        };
    }

    private static void openTag(String format, StringBuilder buf) {
        buf.append("<");
        buf.append(formatToTag(format));
        buf.append(">");
    }

    private static void closeTag(String format, StringBuilder buf) {
        buf.append("</");
        buf.append(formatToTag(format));
        buf.append(">");
    }

    private static void openLink(String href, StringBuilder buf) {
        buf.append("<a href=\"");
        buf.append(Util.he(href));
        buf.append("\">");
    }

    private static void closeLink(StringBuilder buf) {
        buf.append("</a>");
    }

}
