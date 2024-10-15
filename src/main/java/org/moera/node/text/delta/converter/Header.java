package org.moera.node.text.delta.converter;

import java.util.Map;
import java.util.Objects;

public class Header extends Paragraph {

    private final int level;

    public Header(int level, int quoteLevel) {
        super(quoteLevel);
        this.level = level;
    }

    @Override
    public boolean continuesWith(Map<String, Object> lineAttributes) {
        return lineAttributes != null && Objects.equals(lineAttributes.get("header"), level);
    }

    @Override
    public String toHtml() {
        return toHtml("h" + level);
    }

}
