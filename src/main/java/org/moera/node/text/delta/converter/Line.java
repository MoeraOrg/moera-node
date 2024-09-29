package org.moera.node.text.delta.converter;

import org.moera.node.text.delta.model.AttributeMap;
import org.moera.node.text.delta.model.Delta;

public class Line {

    private final Delta delta;
    private final AttributeMap attributes;

    public Line(Delta delta, AttributeMap attributes) {
        this.delta = delta;
        this.attributes = attributes;
    }

    public Delta getDelta() {
        return delta;
    }

    public AttributeMap getAttributes() {
        return attributes;
    }

}
