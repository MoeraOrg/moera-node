package org.moera.node.data;

public enum StoryType {

    POSTING_ADDED,
    REACTION_ADDED_POSITIVE,
    REACTION_ADDED_NEGATIVE,
    MENTION_POSTING;

    public String getValue() {
        return name().toLowerCase().replace('_', '-');
    }

    public static StoryType forValue(String value) {
        String name = value.toUpperCase().replace('-', '_');
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
