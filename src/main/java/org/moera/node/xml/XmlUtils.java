package org.moera.node.xml;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.ObjectUtils;
import org.xml.sax.Attributes;

public class XmlUtils {

    private static final Pattern AMPS_PATTERN = Pattern.compile("&(?:#[0-9]{1,5}|#x[0-9A-Fa-f]{1,4}|[A-Za-z]+);");
    private static final Pattern AMPS_XML_PATTERN = Pattern.compile("&(?:#[0-9]{1,5}|#x[0-9A-Fa-f]{1,4}|lt|amp|quot);");

    public static String delicateSpecialChars(String s) {
        return delicateSpecialChars(s, false, false);
    }

    public static String delicateSpecialChars(String s, boolean gt, boolean sq) {
        s = s.replace("<", "&lt;");
        s = s.replace("\"", "&quot;");
        if (gt) {
            s = s.replace(">", "&gt;");
        }
        if (sq) {
            s = s.replace("'", "&#39;");
        }
        return s;
    }

    public static CharSequence delicateAmps(String s) {
        return delicateAmps(s, true);
    }

    public static CharSequence delicateAmps(String s, boolean xmlEntities) {
        if (ObjectUtils.isEmpty(s)) {
            return s;
        }

        StringBuilder buf = new StringBuilder();
        Pattern pattern = xmlEntities ? AMPS_XML_PATTERN : AMPS_PATTERN;
        Matcher matcher = pattern.matcher(s);
        int i = 0;
        while (matcher.find()) {
            buf.append(s.substring(i, matcher.start()).replace("&", "&amp;"));
            buf.append(matcher.group());
            i = matcher.end();
        }
        buf.append(s.substring(i).replace("&", "&amp;"));
        return buf;
    }

    public static CharSequence makeTag(String name) {
        return makeTag(name, (Attributes) null, false);
    }

    public static CharSequence makeTag(String name, Attributes attributes) {
        return makeTag(name, attributes, false);
    }

    public static CharSequence makeTag(String name, Attributes attributes, boolean isEmpty) {
        StringBuilder buf = new StringBuilder();
        buf.append('<');
        buf.append(name.toLowerCase());
        if (attributes != null) {
            for (int i = 0; i < attributes.getLength(); i++) {
                String localName = attributes.getLocalName(i).toLowerCase();
                if (localName.startsWith("on")) { // filtering out event handlers that may be present in user input
                    continue;
                }
                buf.append(' ');
                buf.append(localName);
                buf.append("=\"");
                buf.append(delicateSpecialChars(attributes.getValue(i)));
                buf.append('"');
            }
        }
        buf.append(isEmpty ? " />" : ">");
        return buf;
    }

    public static CharSequence makeTag(String name, Map<String, String> attributes) {
        return makeTag(name, attributes, false);
    }

    public static CharSequence makeTag(String name, Map<String, String> attributes, boolean isEmpty) {
        StringBuilder buf = new StringBuilder();
        buf.append('<');
        buf.append(name.toLowerCase());
        if (attributes != null) {
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                buf.append(' ');
                buf.append(entry.getKey());
                buf.append("=\"");
                buf.append(delicateSpecialChars(entry.getValue()));
                buf.append('"');
            }
        }
        buf.append(isEmpty ? " />" : ">");
        return buf;
    }

    public static String makeText(String text) {
        return delicateSpecialChars(text, true, false);
    }

    public static boolean hasMarkup(String s) {
        for (int i = 0; i < s.length(); i++) {
            if ("<>&=~_^[]{}'".indexOf(s.charAt(i)) >= 0) {
                return true;
            }
            if (s.charAt(i) == '/' && (i == 0 || s.charAt(i - 1) == ' ' || s.charAt(i - 1) == ':')) {
                return true;
            }
        }
        return false;
    }

}
