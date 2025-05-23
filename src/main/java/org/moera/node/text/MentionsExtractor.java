package org.moera.node.text;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeFilter;
import org.moera.lib.node.types.body.Body;
import org.springframework.util.ObjectUtils;

public class MentionsExtractor {

    public static Set<String> extract(Body body) {
        if (body.getText() == null) {
            return Collections.emptySet();
        }
        return extract(body.getText());
    }

    private static Set<String> extract(String html) {
        Document document = Jsoup.parseBodyFragment(html);
        Extractor extractor = new Extractor();
        document.filter(extractor);
        return extractor.getResult();
    }

    private static final class Extractor implements NodeFilter {

        private final Set<String> result = new HashSet<>();

        Set<String> getResult() {
            return result;
        }

        @Override
        public FilterResult head(Node node, int depth) {
            if (node instanceof Element) {
                String nodeName = ((Element) node).dataset().get("nodename");
                if (!ObjectUtils.isEmpty(nodeName)) {
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
