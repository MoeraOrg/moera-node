package org.moera.node.text;

class PlainTextConverter {

    private enum LineType {
        START,
        BLANK,
        TEXT
    }

    public static String toHtml(String source) {
        String[] lines = source.split("\n");
        StringBuilder buf = new StringBuilder();
        LineType prev = LineType.START;
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                if (prev == LineType.TEXT) {
                    buf.append("</p>");
                }
                prev = LineType.BLANK;
            } else {
                switch (prev) {
                    case START:
                    case BLANK:
                        buf.append("<p>");
                        break;
                    case TEXT:
                        buf.append("<br/>");
                        break;
                }
                buf.append(escape(line));
                prev = LineType.TEXT;
            }
        }
        if (prev == LineType.TEXT) {
            buf.append("</p>");
        }
        return buf.toString();
    }

    private static String escape(String s) {
        return s.replace("<", "&lt;");
    }

}
