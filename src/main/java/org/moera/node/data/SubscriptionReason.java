package org.moera.node.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SubscriptionReason {

    USER,
    MENTION,
    COMMENT,
    AUTO;

    @JsonValue
    public String getValue() {
        return name().toLowerCase().replace('_', '-');
    }

    public static String toValue(SubscriptionReason reason) {
        return reason != null ? reason.getValue() : null;
    }

    public static SubscriptionReason forValue(String value) {
        try {
            return parse(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @JsonCreator
    public static SubscriptionReason parse(String value) {
        return valueOf(value.toUpperCase().replace('-', '_'));
    }

}
