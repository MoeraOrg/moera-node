package org.moera.node.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum BlockedOperation {

    REACTION,
    COMMENT,
    VISIBILITY,
    INSTANT;

    @JsonValue
    public String getValue() {
        return name().toLowerCase().replace('_', '-');
    }

    public static String toValue(BlockedOperation type) {
        return type != null ? type.getValue() : null;
    }

    public static BlockedOperation forValue(String value) {
        try {
            return parse(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @JsonCreator
    public static BlockedOperation parse(String value) {
        return valueOf(value.toUpperCase().replace('-', '_'));
    }

}
