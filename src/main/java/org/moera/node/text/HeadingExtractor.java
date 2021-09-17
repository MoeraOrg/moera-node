package org.moera.node.text;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeFilter;
import org.moera.node.model.Body;
import org.moera.node.util.Util;
import org.springframework.util.ObjectUtils;

public class HeadingExtractor {

    private static final int HEADING_LENGTH = 80;

    public static String extract(Body body) {
        if (!ObjectUtils.isEmpty(body.getSubject())) {
            return Util.ellipsize(body.getSubject(), HEADING_LENGTH);
        }
        return extract(body.getText());
    }

    private static String extract(String html) {
        Document document = Jsoup.parseBodyFragment(html);
        Extractor extractor = new Extractor();
        document.filter(extractor);
        return extractor.getResult();
    }

    private static class Extractor implements NodeFilter {

        private final StringBuilder result = new StringBuilder();
        private boolean ignoreContent = false;

        String getResult() {
            return result.toString();
        }

        @Override
        public FilterResult head(Node node, int depth) {
            if (!ignoreContent) {
                String text = null;
                if (node instanceof TextNode) {
                    text = clear(((TextNode) node).text().trim());
                } else if (node instanceof Element) {
                    Element element = (Element) node;
                    if (element.normalName().equals("mr-spoiler")) {
                        ignoreContent = true;
                        text = element.hasAttr("title") ? element.attr("title") : "spoiler!";
                        text = "[" + text + "]";
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
                if (result.length() >= HEADING_LENGTH) {
                    Util.ellipsize(result, HEADING_LENGTH);
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
                if (element.normalName().equals("mr-spoiler")) {
                    ignoreContent = false;
                }
            }
            return FilterResult.CONTINUE;
        }

    }

}
