package org.moera.node.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum VerificationStatus {

    RUNNING,
    CORRECT,
    INCORRECT,
    ERROR;

    @JsonValue
    public String getValue() {
        return name().toLowerCase().replace('_', '-');
    }

    public static String toValue(VerificationStatus status) {
        return status != null ? status.getValue() : null;
    }

    public static VerificationStatus forValue(String value) {
        try {
            return parse(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @JsonCreator
    public static VerificationStatus parse(String value) {
        return valueOf(value.toUpperCase().replace('-', '_'));
    }

}
