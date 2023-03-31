package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SheriffOrderReason {

    UNLAWFUL,
    DEFAMATORY,
    THREAT,
    SPAM,
    SCAM,
    MALWARE,
    COPYRIGHT,
    IMPERSONATING,
    PRIVACY,
    SLANDER;

    @JsonValue
    public String getValue() {
        return name().toLowerCase().replace('_', '-');
    }

    public static String toValue(SheriffOrderReason category) {
        return category != null ? category.getValue() : null;
    }

    public static SheriffOrderReason forValue(String value) {
        try {
            return parse(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return getValue();
    }

    @JsonCreator
    public static SheriffOrderReason parse(String value) {
        return valueOf(value.toUpperCase().replace('-', '_'));
    }

}
