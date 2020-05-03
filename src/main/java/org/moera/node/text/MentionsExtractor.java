package org.moera.node.text;

import java.util.HashSet;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeFilter;
import org.moera.node.model.Body;
import org.springframework.util.StringUtils;

public class MentionsExtractor {

    public static Set<String> extract(Body body) {
        return extract(body.getText());
    }

    private static Set<String> extract(String html) {
        Document document = Jsoup.parseBodyFragment(html);
        Extractor extractor = new Extractor();
        document.filter(extractor);
        return extractor.getResult();
    }

    private static class Extractor implements NodeFilter {

        private Set<String> result = new HashSet<>();

        Set<String> getResult() {
            return result;
        }

        @Override
        public FilterResult head(Node node, int depth) {
            if (node instanceof Element) {
                String nodeName = ((Element) node).dataset().get("nodename");
                if (!StringUtils.isEmpty(nodeName)) {
                    result.add(nodeName);
                }
            }
            return FilterResult.CONTINUE;
        }

        @Override
        public FilterResult tail(Node node, int depth) {
            return FilterResult.CONTINUE;
        }

    }

}
