package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SheriffOrderCategory {

    VISIBILITY;

    @JsonValue
    public String getValue() {
        return name().toLowerCase().replace('_', '-');
    }

    public static String toValue(SheriffOrderCategory category) {
        return category != null ? category.getValue() : null;
    }

    public static SheriffOrderCategory forValue(String value) {
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
    public static SheriffOrderCategory parse(String value) {
        return valueOf(value.toUpperCase().replace('-', '_'));
    }

}
