package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PushRelayType {

    FCM;

    @JsonValue
    public String getValue() {
        return name().toLowerCase().replace('_', '-');
    }

    public static String toValue(PushRelayType type) {
        return type != null ? type.getValue() : null;
    }

    public static PushRelayType forValue(String value) {
        try {
            return parse(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @JsonCreator
    public static PushRelayType parse(String value) {
        return valueOf(value.toUpperCase().replace('-', '_'));
    }

}
