package org.moera.node.text.shorten;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeFilter;

class Measurer implements NodeFilter {

    private static final String SENTENCE_END = ".?!";
    private static final String PHRASE_END = ":,;)";

    private int textMin;
    private int textAvg;
    private int textMax;

    private int offset;
    private int paragraphCut = -1;
    private int sentenceCut = -1;
    private int phraseCut = -1;
    private int wordCut = -1;
    private boolean textShort = true;

    Measurer(int textMin, int textAvg, int textMax) {
        this.textMin = textMin;
        this.textAvg = textAvg;
        this.textMax = textMax;
    }

    public int getCut() {
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
        return textMin;
    }

    public boolean isTextShort() {
        return textShort;
    }

    public boolean needsEllipsis() {
        return paragraphCut <= 0 && sentenceCut <= 0;
    }

    private int closest(int incumbent, int challenger) {
        if (challenger < textMin || challenger > textMax) {
            return incumbent;
        }
        return incumbent < 0 || Math.abs(challenger - textAvg) < Math.abs(incumbent - textAvg)
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
        if (offset <= textMax) {
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
