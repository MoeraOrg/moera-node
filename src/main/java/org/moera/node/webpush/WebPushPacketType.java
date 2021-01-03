package org.moera.node.webpush;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum WebPushPacketType {

    STORY_ADDED,
    STORY_DELETED;

    @JsonValue
    public String getValue() {
        return name().toLowerCase().replace('_', '-');
    }

    public static String toValue(WebPushPacketType type) {
        return type != null ? type.getValue() : null;
    }

    public static WebPushPacketType forValue(String value) {
        try {
            return parse(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @JsonCreator
    public static WebPushPacketType parse(String value) {
        return valueOf(value.toUpperCase().replace('-', '_'));
    }

}
