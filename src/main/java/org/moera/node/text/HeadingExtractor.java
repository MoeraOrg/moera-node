package org.moera.node.text;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeFilter;
import org.moera.node.model.Body;

public class HeadingExtractor {

    private static final int HEADING_LENGTH = 40;

    public static String extract(Body body) {
        return extract(body.getText());
    }

    private static String extract(String html) {
        Document document = Jsoup.parseBodyFragment(html);
        Extractor extractor = new Extractor();
        document.filter(extractor);
        return extractor.getResult();
    }

    private static class Extractor implements NodeFilter {

        private StringBuilder result = new StringBuilder();

        String getResult() {
            return result.toString();
        }

        @Override
        public FilterResult head(Node node, int depth) {
            if (node instanceof TextNode) {
                String text = ((TextNode) node).text().trim();
                if (!text.isEmpty()) {
                    if (result.length() > 0) {
                        result.append(' ');
                    }
                    result.append(clear(text));
                    if (result.length() >= HEADING_LENGTH) {
                        result.setLength(HEADING_LENGTH);
                        result.append('\u2026');
                        return FilterResult.STOP;
                    }
                }
            }
            return FilterResult.CONTINUE;
        }

        private String clear(String text) {
            return text.replace("\n", " ").replace("\r", "");
        }

        @Override
        public FilterResult tail(Node node, int depth) {
            return FilterResult.CONTINUE;
        }

    }

}
