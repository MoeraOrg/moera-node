package org.moera.node.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ConnectivityStatus {

    NORMAL,
    FROZEN,
    FAILING;

    @JsonValue
    public String getValue() {
        return name().toLowerCase().replace('_', '-');
    }

    public static ConnectivityStatus forValue(String value) {
        String name = value.toUpperCase();
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @JsonCreator
    public static ConnectivityStatus parse(String value) {
        return valueOf(value.toUpperCase().replace('-', '_'));
    }

}
