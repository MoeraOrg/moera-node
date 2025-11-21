package org.moera.node.model;

import org.moera.lib.node.types.StoryType;

public class StoryTypeUtil {

     public static int priority(StoryType storyType) {
          return switch (storyType) {
              case POSTING_ADDED,
                   REACTION_ADDED_POSITIVE,
                   REACTION_ADDED_NEGATIVE,
                   COMMENT_REACTION_ADDED_POSITIVE,
                   COMMENT_REACTION_ADDED_NEGATIVE,
                   COMMENT_POST_TASK_FAILED,
                   COMMENT_UPDATE_TASK_FAILED,
                   POSTING_UPDATED,
                   POSTING_POST_TASK_FAILED,
                   POSTING_UPDATE_TASK_FAILED,
                   POSTING_MEDIA_REACTION_ADDED_POSITIVE,
                   POSTING_MEDIA_REACTION_ADDED_NEGATIVE,
                   COMMENT_MEDIA_REACTION_ADDED_POSITIVE,
                   COMMENT_MEDIA_REACTION_ADDED_NEGATIVE,
                   POSTING_MEDIA_REACTION_FAILED,
                   COMMENT_MEDIA_REACTION_FAILED,
                   POSTING_SUBSCRIBE_TASK_FAILED,
                   POSTING_REACTION_TASK_FAILED,
                   COMMENT_REACTION_TASK_FAILED,
                   ASKED_TO_SUBSCRIBE,
                   ASKED_TO_FRIEND,
                   SHERIFF_MARKED,
                   SHERIFF_UNMARKED,
                   SHERIFF_COMPLAINT_ADDED,
                   SHERIFF_COMPLAINT_DECIDED,
                   DEFROSTING,
                   SEARCH_REPORT,
                   REMINDER_AVATAR -> 0;

              case MENTION_POSTING,
                   MENTION_COMMENT,
                   REMINDER_FULL_NAME -> 1;

              case COMMENT_ADDED,
                   REMOTE_COMMENT_ADDED,
                   REPLY_COMMENT,
                   BLOCKED_USER_IN_POSTING,
                   UNBLOCKED_USER_IN_POSTING,
                   REMINDER_EMAIL -> 2;

              case SUBSCRIBER_ADDED,
                   SUBSCRIBER_DELETED,
                   FRIEND_ADDED,
                   FRIEND_DELETED,
                   FRIEND_GROUP_DELETED,
                   BLOCKED_USER,
                   UNBLOCKED_USER,
                   REMINDER_SHERIFF_ALLOW -> 3;
          };
     }

     public static boolean isReminder(StoryType storyType) {
          return storyType.name().startsWith("REMINDER_");
     }

}
