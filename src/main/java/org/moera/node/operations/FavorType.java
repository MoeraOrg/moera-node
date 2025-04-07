package org.moera.node.operations;

public enum FavorType {

    POST(10f, 14 * 24),
    LIKE_POST(1f, 3 * 24),
    COMMENT(10f, 7 * 24),
    LIKE_COMMENT(.25f, 3 * 24),
    REPLY_TO_COMMENT(5f, 7 * 24),
    SUBSCRIBE_TO_COMMENTS(1f, 3 * 24),

    UNPOST(-10f, 14 * 24),
    UNLIKE_POST(-1f, 3 * 24),
    UNCOMMENT(-10f, 7 * 24),
    UNLIKE_COMMENT(-.25f, 3 * 24),
    UNREPLY_TO_COMMENT(-5f, 7 * 24),
    UNSUBSCRIBE_TO_COMMENTS(-1f, 3 * 24);

    private final float value;
    private final int decayHours;

    FavorType(float value, int decayHours) {
        this.value = value;
        this.decayHours = decayHours;
    }

    public float getValue() {
        return value;
    }

    public int getDecayHours() {
        return decayHours;
    }

}
