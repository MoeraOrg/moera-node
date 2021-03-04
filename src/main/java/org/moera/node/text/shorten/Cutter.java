package org.moera.node.text.shorten;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeFilter;

class Cutter implements NodeFilter {

    private TextPosition cut;
    private boolean ellipsis;
    private Element target;
    private TextPosition offset = new TextPosition();
    private Element ignoreContent = null;

    Cutter(TextPosition cut, boolean ellipsis, Element target) {
        this.cut = cut;
        this.ellipsis = ellipsis;
        this.target = target;
    }

    @Override
    public FilterResult head(Node node, int i) {
        boolean goOn = true;

        if (ignoreContent == null) {
            if (node instanceof TextNode) {
                String text = ((TextNode) node).getWholeText();
                if (offset.plus(text.length()).less(cut)) {
                    target.appendChild(node.clone());
                } else {
                    int len = cut.distance(offset);
                    target.appendChild(new TextNode(len < text.length() ? text.substring(0, len) : text));
                }
                offset = offset.plus(text.length());
                if (offset.greaterOrEquals(cut) && ellipsis) {
                    target.appendChild(new TextNode("\u2026"));
                }
                goOn = offset.less(cut);
            } else if (node instanceof Element) {
                Element element = (Element) node;
                if (Elements.isDetails(element)) {
                    ignoreContent = element;
                } else if (Elements.isObject(element)) {
                    ignoreContent = element;
                    goOn = offset.space(Elements.getHeight(element)).lessOrEquals(cut);
                }
            }
        }

        if (goOn && node instanceof Element && !Elements.isBody((Element) node)) {
            Element sub = new Element(((Element) node).tag(), "", node.attributes());
            target.appendChild(sub);
            target = sub;
        }

        return goOn ? FilterResult.CONTINUE : FilterResult.STOP;
    }

    @Override
    public FilterResult tail(Node node, int i) {
        if (node instanceof Element) {
            Element element = (Element) node;
            if (ignoreContent == null) {
                if (Elements.isBreaking(element)) {
                    offset = offset.newLine();
                }
            } else if (ignoreContent == node) {
                ignoreContent = null;
                if (Elements.isObject(element)) {
                    offset = offset.space(Elements.getHeight(element));
                }
            }
            if (!Elements.isBody(element)) {
                target = target.parent();
            }
        }
        return FilterResult.CONTINUE;
    }

}
