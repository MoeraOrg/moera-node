package org.moera.node.text.delta.converter;

public class HorizontalRule extends Block {

    public HorizontalRule(int quoteLevel) {
        super(quoteLevel);
    }

    @Override
    public String toHtml() {
        return "<hr>";
    }

}
