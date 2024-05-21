package org.moera.node.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum StoryType {

    POSTING_ADDED(0),                         // 0
    REACTION_ADDED_POSITIVE(0),               // 1
    REACTION_ADDED_NEGATIVE(0),               // 2
    MENTION_POSTING(2),                       // 3
    SUBSCRIBER_ADDED(3),                      // 4
    SUBSCRIBER_DELETED(3),                    // 5
    COMMENT_ADDED(1),                         // 6
    MENTION_COMMENT(2),                       // 7
    REPLY_COMMENT(2),                         // 8
    COMMENT_REACTION_ADDED_POSITIVE(0),       // 9
    COMMENT_REACTION_ADDED_NEGATIVE(0),       // 10
    REMOTE_COMMENT_ADDED(1),                  // 11
    COMMENT_POST_TASK_FAILED(0),              // 12
    COMMENT_UPDATE_TASK_FAILED(0),            // 13
    POSTING_UPDATED(0),                       // 14
    POSTING_POST_TASK_FAILED(0),              // 15
    POSTING_UPDATE_TASK_FAILED(0),            // 16
    POSTING_MEDIA_REACTION_ADDED_POSITIVE(0), // 17
    POSTING_MEDIA_REACTION_ADDED_NEGATIVE(0), // 18
    COMMENT_MEDIA_REACTION_ADDED_POSITIVE(0), // 19
    COMMENT_MEDIA_REACTION_ADDED_NEGATIVE(0), // 20
    POSTING_MEDIA_REACTION_FAILED(0),         // 21
    COMMENT_MEDIA_REACTION_FAILED(0),         // 22
    POSTING_SUBSCRIBE_TASK_FAILED(0),         // 23
    POSTING_REACTION_TASK_FAILED(0),          // 24
    COMMENT_REACTION_TASK_FAILED(0),          // 25
    FRIEND_ADDED(3),                          // 26
    FRIEND_DELETED(3),                        // 27
    FRIEND_GROUP_DELETED(3),                  // 28
    ASKED_TO_SUBSCRIBE(0),                    // 29
    ASKED_TO_FRIEND(0),                       // 30
    BLOCKED_USER(3),                          // 31
    UNBLOCKED_USER(3),                        // 32
    BLOCKED_USER_IN_POSTING(2),               // 33
    UNBLOCKED_USER_IN_POSTING(2),             // 34
    SHERIFF_MARKED(0),                        // 35
    SHERIFF_UNMARKED(0),                      // 36
    SHERIFF_COMPLAIN_ADDED(0),                // 37
    SHERIFF_COMPLAIN_DECIDED(0);              // 38

    private final int priority;

    StoryType(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }

    @JsonValue
    public String getValue() {
        return name().toLowerCase().replace('_', '-');
    }

    public static String toValue(StoryType type) {
        return type != null ? type.getValue() : null;
    }

    public static StoryType forValue(String value) {
        try {
            return parse(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @JsonCreator
    public static StoryType parse(String value) {
        return valueOf(value.toUpperCase().replace('-', '_'));
    }

}
