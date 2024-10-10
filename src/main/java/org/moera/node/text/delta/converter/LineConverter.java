package org.moera.node.text.delta.converter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.moera.commons.util.UniversalLocation;
import org.moera.naming.rpc.NodeName;
import org.moera.node.text.delta.model.AttributeMap;
import org.moera.node.text.delta.model.Delta;
import org.moera.node.text.delta.model.Op;
import org.moera.node.util.Util;

public class LineConverter {

    private record Format(String name, Object value) {
    }

    private static final Set<String> KNOWN_FORMATS = Set.of("bold", "code", "italic", "mention", "strike", "underline");

    public static String toHtml(Delta line) {
        StringBuilder buf = new StringBuilder();

        String openLink = null;
        Set<Format> openFormats = new HashSet<>();
        Deque<Format> openStack = new ArrayDeque<>();
        Set<String> toClose = new HashSet<>();
        List<Format> toOpen = new ArrayList<>();

        for (Op op : line.getOps()) {
            if (!op.isInsert()) {
                continue;
            }

            AttributeMap attrs = op.attributes();
            if (attrs == null) {
                attrs = new AttributeMap();
            }

            toClose.clear();
            for (Format format : openFormats) {
                if (!attrs.containsKey(format.name) || !Objects.equals(attrs.get(format.name), format.value)) {
                    toClose.add(format.name);
                }
            }

            toOpen.clear();
            for (String formatName : attrs.keySet()) {
                Format format = new Format(formatName, attrs.get(formatName));
                if (KNOWN_FORMATS.contains(format.name) && !openFormats.contains(format)) {
                    toOpen.add(format);
                }
            }

            String newLink = (String) attrs.get("link");

            while (!openStack.isEmpty()) {
                if (toClose.isEmpty() && Objects.equals(openLink, newLink)) {
                    break;
                }

                Format format = openStack.pop();
                openFormats.remove(format);
                if (toClose.contains(format.name)) {
                    toClose.remove(format.name);
                    closeTag(format.name, buf);
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
                Format format = toOpen.get(i);
                openTag(format, buf);
                openStack.push(format);
                openFormats.add(format);
            }

            buf.append(op.argAsString());
        }

        return buf.toString();
    }

    private static String formatToTag(Format format) {
        return switch (format.name) {
            case "bold" -> "b";
            case "code" -> "code";
            case "italic" -> "i";
            case "mention" -> {
                String name = (String) format.value;
                if (format.value != null) {
                    yield String.format("a href=\"%s\" data-nodename=\"%s\"",
                            UniversalLocation.redirectTo(name, null), NodeName.expand(name));
                } else {
                    yield "a";
                }
            }
            case "strike" -> "s";
            case "underline" -> "u";
            default -> null;
        };
    }

    private static void openTag(Format format, StringBuilder buf) {
        buf.append("<");
        buf.append(formatToTag(format));
        buf.append(">");
    }

    private static void closeTag(String formatName, StringBuilder buf) {
        buf.append("</");
        buf.append(formatToTag(new Format(formatName, null)));
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
