package org.moera.node.text.delta.converter;

import java.util.Objects;

import org.moera.node.text.delta.model.AttributeMap;

public class Header extends Paragraph {

    private final int level;

    public Header(int level, int quoteLevel) {
        super(quoteLevel);
        this.level = level;
    }

    @Override
    public boolean continuesWith(AttributeMap lineAttributes) {
        return lineAttributes != null && Objects.equals(lineAttributes.get("header"), level);
    }

    @Override
    public String toHtml() {
        return toHtml("h" + level);
    }

}
