package org.moera.node.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DraftType {

    NEW_POSTING,
    POSTING_UPDATE,
    NEW_COMMENT,
    COMMENT_UPDATE;

    @JsonValue
    public String getValue() {
        return name().toLowerCase().replace('_', '-');
    }

    public static String toValue(DraftType type) {
        return type != null ? type.getValue() : null;
    }

    public static DraftType forValue(String value) {
        try {
            return parse(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @JsonCreator
    public static DraftType parse(String value) {
        return valueOf(value.toUpperCase().replace('-', '_'));
    }

}
