package org.moera.node.data;

public enum SourceFormat {

    PLAIN_TEXT,
    HTML;

    public String getValue() {
        return name().toLowerCase().replace('_', '-');
    }

    public static SourceFormat forValue(String value) {
        String name = value.toUpperCase().replace('-', '_');
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
