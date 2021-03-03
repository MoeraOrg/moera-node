package org.moera.node.text.shorten;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeFilter;

class Cutter implements NodeFilter {

    private int cut;
    private boolean ellipsis;
    private Element target;
    private int offset;

    Cutter(int cut, boolean ellipsis, Element target) {
        this.cut = cut;
        this.ellipsis = ellipsis;
        this.target = target;
    }

    @Override
    public FilterResult head(Node node, int i) {
        if (node instanceof TextNode) {
            String text = ((TextNode) node).getWholeText();
            if (offset + text.length() < cut) {
                target.appendChild(node.clone());
            } else {
                target.appendChild(new TextNode(text.substring(0, cut - offset)));
            }
            offset += text.length();
            if (offset >= cut && ellipsis) {
                target.appendChild(new TextNode("\u2026"));
            }
        }
        if (node instanceof Element && !((Element) node).normalName().equals("body")) {
            Element sub = new Element(((Element) node).tag(), "", node.attributes());
            target.appendChild(sub);
            target = sub;
        }
        return offset < cut ? FilterResult.CONTINUE : FilterResult.STOP;
    }

    @Override
    public FilterResult tail(Node node, int i) {
        if (node instanceof Element && !((Element) node).normalName().equals("body")) {
            target = target.parent();
        }
        return FilterResult.CONTINUE;
    }

}
