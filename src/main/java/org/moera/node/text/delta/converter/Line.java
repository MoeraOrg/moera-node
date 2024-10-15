package org.moera.node.text.delta.converter;

import java.util.Map;

import org.moera.node.text.delta.model.Delta;

public class Line {

    private final Delta delta;
    private final Map<String, Object> attributes;

    public Line(Delta delta, Map<String, Object> attributes) {
        this.delta = delta;
        this.attributes = attributes;
    }

    public Delta getDelta() {
        return delta;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

}
