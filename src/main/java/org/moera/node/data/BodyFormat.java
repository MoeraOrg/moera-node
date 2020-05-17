package org.moera.node.data;

public enum BodyFormat {

    MESSAGE,
    APPLICATION;

    public String getValue() {
        return name().toLowerCase();
    }

    public static BodyFormat forValue(String value) {
        String name = value.toUpperCase();
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
