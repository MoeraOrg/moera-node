package org.moera.node.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SheriffComplaintStatus {

    POSTED,
    PREPARED,
    PREPARE_FAILED,
    NOT_FOUND,
    INVALID_TARGET,
    NOT_ORIGINAL,
    NOT_SHERIFF,
    APPROVED,
    REJECTED;

    @JsonValue
    public String getValue() {
        return name().toLowerCase().replace('_', '-');
    }

    public static String toValue(SheriffComplaintStatus reason) {
        return reason != null ? reason.getValue() : null;
    }

    public static SheriffComplaintStatus forValue(String value) {
        try {
            return parse(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @JsonCreator
    public static SheriffComplaintStatus parse(String value) {
        return valueOf(value.toUpperCase().replace('-', '_'));
    }

}
