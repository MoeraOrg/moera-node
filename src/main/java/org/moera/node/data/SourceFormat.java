package org.moera.node.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SourceFormat {

    PLAIN_TEXT,
    HTML,
    MARKDOWN,
    APPLICATION,
    VISUAL_HTML;

    @JsonValue
    public String getValue() {
        return name().toLowerCase().replace('_', '-');
    }

    public static String toValue(SourceFormat type) {
        return type != null ? type.getValue() : null;
    }

    public static SourceFormat forValue(String value) {
        try {
            return parse(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @JsonCreator
    public static SourceFormat parse(String value) {
        return valueOf(value.toUpperCase().replace('-', '_'));
    }

}
