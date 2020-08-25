package org.moera.node.text;

import org.intellij.lang.annotations.Language;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeVisitor;

import java.util.ArrayList;
import java.util.List;

public class HtmlProcessor {
    @Language("HTML")
    public static String process(@Language("HTML") String source) {
        Document document = Jsoup.parseBodyFragment(source);

        SpoilerCollector spoilerCollector = new SpoilerCollector();
        document.traverse(spoilerCollector);
        // We are not replacing spoilers during traversing, because we can't change node we're traversing
        for (Element spoiler : spoilerCollector.spoilers) {
            Element details = new Element("details").addClass("spoiler");
            if (spoiler.childrenSize() == 0 || !"summary".equals(spoiler.child(0).tagName())) {
                details.appendChild(new Element("summary").text("spoiler!"));
            }
            spoiler.replaceWith(details.insertChildren(details.childNodeSize(), spoiler.childNodes()));
        }
        return document.body().html();
    }

    private static class SpoilerCollector implements NodeVisitor {
        List<Element> spoilers = new ArrayList<>();

        @Override
        public void head(Node node, int depth) {
            if (node instanceof Element && "spoiler".equals(node.nodeName())) {
                spoilers.add((Element) node);
            }
        }

        @Override
        public void tail(Node node, int depth) {

        }
    }
}
