package org.moera.node.text;

import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeFilter;
import org.moera.node.model.body.Body;
import org.moera.node.util.Util;
import org.springframework.util.ObjectUtils;

public class HeadingExtractor {

    private static final int HEADING_LENGTH = 80;
    private static final int DESCRIPTION_LENGTH = 200;

    private static final String EMOJI_PICTURE = Character.toString(0x1f5bc);
    private static final String EMOJI_SCROLL = Character.toString(0x1f4dc);
    private static final String EMOJI_CHAIN = Character.toString(0x1f517);

    private static final Pattern URL = Pattern.compile(
            "https?://[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9]{1,6}"
                    + "\\b(?:[-a-zA-Z0-9()!@:%_+.~#?&/=]*[-a-zA-Z0-9@:%_+~#&/=])?",
            Pattern.CASE_INSENSITIVE);

    public static String extractHeading(Body body, boolean hasGallery, boolean collapseQuotations) {
        if (!ObjectUtils.isEmpty(body.getSubject())) {
            return Util.ellipsize(body.getSubject(), HEADING_LENGTH);
        }
        String text = URL.matcher(body.getText()).replaceAll(EMOJI_CHAIN);
        String heading = extract(text, HEADING_LENGTH, collapseQuotations);
        if (heading.length() < HEADING_LENGTH && hasGallery) {
            heading += EMOJI_PICTURE;
        }
        return heading;
    }

    public static String extractDescription(Body body) {
        if (ObjectUtils.isEmpty(body.getText())) {
            return "";
        }
        return extract(body.getText(), DESCRIPTION_LENGTH, false);
    }

    private static String extract(String html, int len, boolean collapseQuotations) {
        Document document = Jsoup.parseBodyFragment(html);
        Extractor extractor = new Extractor(len, collapseQuotations);
        document.filter(extractor);
        return extractor.getResult();
    }

    private static class Extractor implements NodeFilter {

        private final StringBuilder result = new StringBuilder();
        private int ignoreContent = 0;
        private final int len;
        private final boolean collapseQuotations;

        Extractor(int len, boolean collapseQuotations) {
            this.len = len;
            this.collapseQuotations = collapseQuotations;
        }

        String getResult() {
            return result.toString();
        }

        @Override
        public FilterResult head(Node node, int depth) {
            if (ignoreContent <= 0) {
                String text = null;
                if (node instanceof TextNode) {
                    text = clear(((TextNode) node).text().trim());
                } else if (node instanceof Element) {
                    Element element = (Element) node;
                    if (element.normalName().equals("mr-spoiler")) {
                        ignoreContent++;
                        text = element.hasAttr("title") ? element.attr("title") : "spoiler!";
                        text = "[" + text + "]";
                    } else if (element.normalName().equals("img")) {
                        text = EMOJI_PICTURE;
                    } else if (element.normalName().equals("blockquote") && collapseQuotations) {
                        text = EMOJI_SCROLL;
                        ignoreContent++;
                    }
                }
                if (appendText(text)) {
                    return FilterResult.STOP;
                }
            }
            return FilterResult.CONTINUE;
        }

        private boolean appendText(String text) {
            if (!ObjectUtils.isEmpty(text)) {
                if (result.length() > 0) {
                    result.append(' ');
                }
                result.append(text);
                if (result.length() >= len) {
                    Util.ellipsize(result, len);
                    return true;
                }
            }
            return false;
        }

        private String clear(String text) {
            return text.replace("\n", " ").replace("\r", "");
        }

        @Override
        public FilterResult tail(Node node, int depth) {
            if (node instanceof Element) {
                Element element = (Element) node;
                if (element.normalName().equals("mr-spoiler")
                        || element.normalName().equals("blockquote") && collapseQuotations) {
                    ignoreContent--;
                }
            }
            return FilterResult.CONTINUE;
        }

    }

}
