package org.moera.node.text.delta.converter;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
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

    private static final Map<String, Integer> KNOWN_FORMATS = Map.of(
        "bold", 1,
        "code", 2,
        "italic", 1,
        "link", 3,
        "mention", 0,
        "spoiler", 4,
        "strike", 1,
        "underline", 1
    );

    public static String toHtml(Delta line) {
        StringBuilder buf = new StringBuilder();

        Set<Format> openFormats = new HashSet<>();
        Deque<Format> openStack = new ArrayDeque<>();
        Set<String> toClose = new HashSet<>();
        Queue<Format> toOpen = new PriorityQueue<>((f1, f2) -> {
            int p1 = KNOWN_FORMATS.get(f1.name);
            int p2 = KNOWN_FORMATS.get(f2.name);
            return p1 != p2 ? p2 - p1 : f2.name.compareTo(f1.name);
        });

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
                if (KNOWN_FORMATS.containsKey(format.name) && !openFormats.contains(format)) {
                    toOpen.add(format);
                }
            }

            while (!openStack.isEmpty()) {
                int toOpenPriority = !toOpen.isEmpty() ? KNOWN_FORMATS.get(toOpen.peek().name) : 0;
                assert openStack.peek() != null;
                if (toClose.isEmpty() && KNOWN_FORMATS.get(openStack.peek().name) >= toOpenPriority) {
                    break;
                }

                Format format = openStack.pop();
                openFormats.remove(format);
                closeTag(format.name, buf);
                if (toClose.contains(format.name)) {
                    toClose.remove(format.name);
                } else {
                    toOpen.add(format);
                }
            }

            while (!toOpen.isEmpty()) {
                Format format = toOpen.poll();
                openTag(format, buf);
                openStack.push(format);
                openFormats.add(format);
            }

            buf.append(op.argAsString());
        }

        while (!openStack.isEmpty()) {
            closeTag(openStack.pop().name, buf);
        }

        return buf.toString();
    }

    private static String formatToTag(Format format) {
        return switch (format.name) {
            case "bold" -> "b";
            case "code" -> "code";
            case "italic" -> "i";
            case "link" -> {
                String href = (String) format.value;
                if (href != null) {
                    yield String.format("a href=\"%s\"", Util.he(href));
                } else {
                    yield "a";
                }
            }
            case "mention" -> {
                String name = (String) format.value;
                if (name != null) {
                    yield String.format("a href=\"%s\" data-nodename=\"%s\"",
                            UniversalLocation.redirectTo(name, null), Util.he(NodeName.expand(name)));
                } else {
                    yield "a";
                }
            }
            case "spoiler" -> {
                String title = (String) format.value;
                if (title != null) {
                    yield String.format("mr-spoiler title=\"%s\"", Util.he(title));
                } else {
                    yield "mr-spoiler";
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

}
