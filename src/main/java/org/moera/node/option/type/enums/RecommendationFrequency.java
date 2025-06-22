package org.moera.node.option.type.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RecommendationFrequency {

    NONE(0),
    MUCH_LESS(0.25f),
    LESS(0.5f),
    NORMAL(1),
    MORE(2),
    MUCH_MORE(4);

    private final float factor;

    RecommendationFrequency(float factor) {
        this.factor = factor;
    }

    public float getFactor() {
        return factor;
    }

    @JsonValue
    public String getValue() {
        return name().toLowerCase().replace("__", "/").replace('_', '-');
    }

    public static String toValue(RecommendationFrequency type) {
        return type != null ? type.getValue() : null;
    }

    public static RecommendationFrequency forValue(String value) {
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
    public static RecommendationFrequency parse(String value) {
        return valueOf(value.toUpperCase().replace('-', '_').replace("/", "__"));
    }

}
