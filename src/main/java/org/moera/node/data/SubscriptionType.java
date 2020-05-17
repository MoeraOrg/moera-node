package org.moera.node.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SubscriptionType {

    FEED,
    POSTING;

    @JsonValue
    public String getValue() {
        return name().toLowerCase().replace('_', '-');
    }

    public static String toValue(SubscriptionType type) {
        return type != null ? type.getValue() : null;
    }

    public static SubscriptionType forValue(String value) {
        try {
            return parse(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @JsonCreator
    public static SubscriptionType parse(String value) {
        return valueOf(value.toUpperCase().replace('-', '_'));
    }

}
