package org.moera.node.push;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PushContentType {

    STORY_ADDED,
    STORY_DELETED,
    FEED_UPDATED;

    @JsonValue
    public String getValue() {
        return name().toLowerCase().replace('_', '-');
    }

    public static String toValue(PushContentType type) {
        return type != null ? type.getValue() : null;
    }

    public static PushContentType forValue(String value) {
        try {
            return parse(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @JsonCreator
    public static PushContentType parse(String value) {
        return valueOf(value.toUpperCase().replace('-', '_'));
    }

}
