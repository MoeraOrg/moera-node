package org.moera.node.text.shorten;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeFilter;

class Measurer implements NodeFilter {

    private static final String SENTENCE_END = ".?!";
    private static final String PHRASE_END = ":,;)";

    private static final TextPosition TEXT_MIN = new TextPosition(4, 0);
    private static final TextPosition TEXT_AVG = new TextPosition(8, 0);
    private static final TextPosition TEXT_MAX = new TextPosition(10, 0);

    private TextPosition offset = new TextPosition();
    private TextPosition paragraphCut = null;
    private TextPosition sentenceCut = null;
    private TextPosition phraseCut = null;
    private TextPosition wordCut = null;
    private Element ignoreContent = null;

    public TextPosition getCut() {
        if (paragraphCut != null) {
            return paragraphCut;
        }
        if (sentenceCut != null) {
            return sentenceCut;
        }
        if (phraseCut != null) {
            return phraseCut;
        }
        if (wordCut != null) {
            return wordCut;
        }
        return TEXT_MIN;
    }

    public boolean isTextShort() {
        return offset.lessOrEquals(TEXT_MAX) || offset.lessOrEquals(getCut());
    }

    public boolean needsEllipsis() {
        return paragraphCut == null && sentenceCut == null;
    }

    private TextPosition closest(TextPosition incumbent, TextPosition challenger) {
        if (challenger.less(TEXT_MIN) || challenger.greater(TEXT_MAX)) {
            return incumbent;
        }
        return incumbent == null || challenger.distance(TEXT_AVG) < incumbent.distance(TEXT_AVG)
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
                    sentenceCut = closest(sentenceCut, offset.plus(i + 1));
                    continue;
                }
                if (PHRASE_END.indexOf(text.charAt(i)) >= 0) {
                    phraseCut = closest(phraseCut, offset.plus(i + 1));
                    continue;
                }
            }
            if (isSpace(text.charAt(i))) {
                wordCut = closest(wordCut, offset.plus(i));
            }
        }
    }

    @Override
    public FilterResult head(Node node, int depth) {
        if (ignoreContent == null) {
            if (node instanceof TextNode) {
                String text = ((TextNode) node).getWholeText();
                scanText(text);
                offset = offset.plus(text.length());
            } else if (node instanceof Element) {
                Element element = (Element) node;
                if (Elements.isDetails(element) || Elements.isObject(element)) {
                    ignoreContent = element;
                }
            }
        }
        if (offset.lessOrEquals(TEXT_MAX)) {
            return FilterResult.CONTINUE;
        } else if (offset.greater(getCut())) {
            return FilterResult.STOP;
        } else {
            return FilterResult.CONTINUE;
        }
    }

    @Override
    public FilterResult tail(Node node, int depth) {
        if (node instanceof Element) {
            Element element = (Element) node;
            if (ignoreContent == null) {
                if (Elements.isBreaking(element)) {
                    offset = offset.newLine();
                    paragraphCut = closest(paragraphCut, offset);
                }
            } else if (ignoreContent == node) {
                ignoreContent = null;
                boolean firstObject = false;
                if (Elements.isObject(element)) {
                    firstObject = offset.less(TEXT_MIN);
                    offset = offset.space(Elements.getHeight(element));
                }
                paragraphCut = firstObject ? offset : closest(paragraphCut, offset);
            }
        }
        return FilterResult.CONTINUE;
    }

}
