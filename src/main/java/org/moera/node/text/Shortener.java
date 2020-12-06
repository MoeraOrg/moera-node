package org.moera.node.text;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeFilter;
import org.moera.node.model.Body;
import org.moera.node.util.Util;

public class Shortener {

    private static final int SHORT_TITLE_MAX = 80;

    private static final int SHORT_TEXT_MIN = 400;
    private static final int SHORT_TEXT_AVG = 700;
    private static final int SHORT_TEXT_MAX = 1000;

    private static final String SENTENCE_END = ".?!";
    private static final String PHRASE_END = ":,;)";

    private static boolean isShort(Body body) {
        return (body.getSubject() == null || body.getSubject().length() <= SHORT_TITLE_MAX)
                && body.getText().length() <= SHORT_TEXT_MAX;
    }

    private static boolean isShortened(Body body, Body shortBody) {
        return body.getSubject() != null && body.getSubject().length() > SHORT_TITLE_MAX
                && shortBody.getSubject() != null && shortBody.getSubject().length() < body.getSubject().length()
                || shortBody.getText() != null /* null is a signal that the text is short enough */;
    }

    public static Body shorten(Body body) {
        if (isShort(body)) {
            return null;
        }

        Body shortBody = new Body();
        if (body.getSubject() != null && body.getSubject().length() > SHORT_TITLE_MAX) {
            shortBody.setSubject(Util.ellipsize(body.getSubject(), SHORT_TITLE_MAX));
        }
        if (body.getText().length() > SHORT_TEXT_MAX) {
            shortBody.setText(shorten(body.getText()));
        }
        return isShortened(body, shortBody) ? shortBody : null;
    }

    private static String shorten(String html) {
        if (html.length() <= SHORT_TEXT_MAX) {
            return null;
        }

        Document document = Jsoup.parseBodyFragment(html);
        Measurer measurer = new Measurer();
        document.body().filter(measurer);

        if (measurer.isTextShort()) {
            return null;
        }

        Document result = Document.createShell("");
        Cutter cutter = new Cutter(measurer.getCut(), measurer.needsEllipsis(), result.body());
        document.body().filter(cutter);
        return result.body().html();
    }

    private static class Measurer implements NodeFilter {

        private int offset;
        private int paragraphCut = -1;
        private int sentenceCut = -1;
        private int phraseCut = -1;
        private int wordCut = -1;
        private boolean textShort = true;

        int getCut() {
            if (paragraphCut > 0) {
                return paragraphCut;
            }
            if (sentenceCut > 0) {
                return sentenceCut;
            }
            if (phraseCut > 0) {
                return phraseCut;
            }
            if (wordCut > 0) {
                return wordCut;
            }
            return SHORT_TEXT_AVG;
        }

        boolean isTextShort() {
            return textShort;
        }

        boolean needsEllipsis() {
            return paragraphCut <= 0 && sentenceCut <= 0;
        }

        private int closest(int incumbent, int challenger) {
            if (challenger < SHORT_TEXT_MIN || challenger > SHORT_TEXT_MAX) {
                return incumbent;
            }
            return incumbent < 0 || Math.abs(challenger - SHORT_TEXT_AVG) < Math.abs(incumbent - SHORT_TEXT_AVG)
                    ? challenger : incumbent;
        }

        private boolean isSpace(char c) {
            switch (Character.getType(c)) {
                case Character.COMBINING_SPACING_MARK:
                case Character.CONTROL:
                case Character.ENCLOSING_MARK:
                case Character.FORMAT:
                case Character.LINE_SEPARATOR:
                case Character.NON_SPACING_MARK:
                case Character.PARAGRAPH_SEPARATOR:
                case Character.PRIVATE_USE:
                case Character.SPACE_SEPARATOR:
                case Character.SURROGATE:
                case Character.UNASSIGNED:
                    return true;
            }
            return false;
        }

        private void scanText(String text) {
            for (int i = 0; i < text.length() - 1; i++) {
                if (i == text.length() - 1 || isSpace(text.charAt(i + 1))) {
                    if (SENTENCE_END.indexOf(text.charAt(i)) >= 0) {
                        sentenceCut = closest(sentenceCut, offset + i + 1);
                        continue;
                    }
                    if (PHRASE_END.indexOf(text.charAt(i)) >= 0) {
                        phraseCut = closest(phraseCut, offset + i + 1);
                        continue;
                    }
                }
                if (isSpace(text.charAt(i))) {
                    wordCut = closest(wordCut, offset + i);
                }
            }
        }

        @Override
        public FilterResult head(Node node, int depth) {
            if (node instanceof TextNode) {
                String text = ((TextNode) node).getWholeText();
                scanText(text);
                offset += text.length();
            }
            if (offset <= SHORT_TEXT_MAX) {
                return FilterResult.CONTINUE;
            } else {
                textShort = false;
                return FilterResult.STOP;
            }
        }

        @Override
        public FilterResult tail(Node node, int depth) {
            if (node instanceof Element && ((Element) node).tag().isBlock()) {
                paragraphCut = closest(paragraphCut, offset);
            }
            return FilterResult.CONTINUE;
        }

    }

    private static class Cutter implements NodeFilter {

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

}
