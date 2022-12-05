package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AskSubject {

    SUBSCRIBE,
    FRIEND;

    @JsonValue
    public String getValue() {
        return name().toLowerCase().replace('_', '-');
    }

    public static AskSubject forValue(String value) {
        String name = value.toUpperCase();
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @JsonCreator
    public static AskSubject parse(String value) {
        return valueOf(value.toUpperCase().replace('-', '_'));
    }

}
