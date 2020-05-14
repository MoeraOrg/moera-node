package org.moera.node.data;

public enum SubscriptionType {

    FEED,
    POSTING;

    public String getValue() {
        return name().toLowerCase().replace('_', '-');
    }

    public static SubscriptionType forValue(String value) {
        String name = value.toUpperCase().replace('-', '_');
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
