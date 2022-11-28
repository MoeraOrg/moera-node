package org.moera.node.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum StoryType {

    POSTING_ADDED,                         // 0
    REACTION_ADDED_POSITIVE,               // 1
    REACTION_ADDED_NEGATIVE,               // 2
    MENTION_POSTING,                       // 3
    SUBSCRIBER_ADDED,                      // 4
    SUBSCRIBER_DELETED,                    // 5
    COMMENT_ADDED,                         // 6
    MENTION_COMMENT,                       // 7
    REPLY_COMMENT,                         // 8
    COMMENT_REACTION_ADDED_POSITIVE,       // 9
    COMMENT_REACTION_ADDED_NEGATIVE,       // 10
    REMOTE_COMMENT_ADDED,                  // 11
    COMMENT_POST_TASK_FAILED,              // 12
    COMMENT_UPDATE_TASK_FAILED,            // 13
    POSTING_UPDATED,                       // 14
    POSTING_POST_TASK_FAILED,              // 15
    POSTING_UPDATE_TASK_FAILED,            // 16
    POSTING_MEDIA_REACTION_ADDED_POSITIVE, // 17
    POSTING_MEDIA_REACTION_ADDED_NEGATIVE, // 18
    COMMENT_MEDIA_REACTION_ADDED_POSITIVE, // 19
    COMMENT_MEDIA_REACTION_ADDED_NEGATIVE, // 20
    POSTING_MEDIA_REACTION_FAILED,         // 21
    COMMENT_MEDIA_REACTION_FAILED,         // 22
    POSTING_SUBSCRIBE_TASK_FAILED,         // 23
    POSTING_REACTION_TASK_FAILED,          // 24
    COMMENT_REACTION_TASK_FAILED,          // 25
    FRIEND_ADDED,                          // 26
    FRIEND_DELETED,                        // 27
    FRIEND_GROUP_DELETED;                  // 28

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
