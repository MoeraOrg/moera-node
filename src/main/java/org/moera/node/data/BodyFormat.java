package org.moera.node.data;

public enum BodyFormat {

    MESSAGE,
    APPLICATION;

    public String getValue() {
        return name().toLowerCase();
    }

    public static BodyFormat forValue(String value) {
        for (BodyFormat bf : values()) {
            if (bf.getValue().equalsIgnoreCase(value)) {
                return bf;
            }
        }
        return null;
    }

}
