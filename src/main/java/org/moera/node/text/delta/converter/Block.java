package org.moera.node.text.delta.converter;

import java.util.Map;

public abstract class Block {

    private final int quoteLevel;

    public Block(int quoteLevel) {
        this.quoteLevel = quoteLevel;
    }

    public int getQuoteLevel() {
        return quoteLevel;
    }

    public boolean continuesWith(Map<String, Object> lineAttributes) {
        return false;
    }

    public void addLine(Line line) {
    }

    public abstract String toHtml();

}
