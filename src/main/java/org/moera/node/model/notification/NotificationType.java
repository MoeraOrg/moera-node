package org.moera.node.model.notification;

public enum NotificationType {

    MENTION_POSTING_ADDED(MentionPostingAddedNotification.class),
    MENTION_POSTING_DELETED(MentionPostingDeletedNotification.class),
    STORY_ADDED(StoryAddedNotification.class),
    POSTING_UPDATED(PostingUpdatedNotification.class),
    POSTING_DELETED(PostingDeletedNotification.class),
    POSTING_REACTIONS_UPDATED(PostingReactionsUpdatedNotification.class),
    MENTION_COMMENT_ADDED(MentionCommentAddedNotification.class),
    MENTION_COMMENT_DELETED(MentionCommentDeletedNotification.class),
    POSTING_COMMENTS_UPDATED(PostingCommentsUpdatedNotification.class),
    REPLY_COMMENT_ADDED(ReplyCommentAddedNotification.class),
    REPLY_COMMENT_DELETED(ReplyCommentDeletedNotification.class),
    COMMENT_REACTION_ADDED(CommentReactionAddedNotification.class),
    COMMENT_REACTION_DELETED(CommentReactionDeletedNotification.class),
    COMMENT_REACTION_DELETED_ALL(CommentReactionDeletedAllNotification.class),
    POSTING_COMMENT_ADDED(PostingCommentAddedNotification.class),
    POSTING_COMMENT_DELETED(PostingCommentDeletedNotification.class),
    PROFILE_UPDATED(ProfileUpdatedNotification.class),
    POSTING_IMPORTANT_UPDATE(PostingImportantUpdateNotification.class),
    POSTING_REACTION_ADDED(PostingReactionAddedNotification.class),
    POSTING_REACTION_DELETED(PostingReactionDeletedNotification.class),
    POSTING_REACTION_DELETED_ALL(PostingReactionDeletedAllNotification.class),
    FRIENDSHIP_UPDATED(FriendshipUpdatedNotification.class),
    FRIEND_GROUP_UPDATED(FriendGroupUpdatedNotification.class),
    FRIEND_GROUP_DELETED(FriendGroupDeletedNotification.class),
    ASKED(AskedNotification.class),
    BLOCKING_ADDED(BlockingAddedNotification.class),
    BLOCKING_DELETED(BlockingDeletedNotification.class),
    SHERIFF_ORDER_FOR_POSTING_ADDED(SheriffOrderForPostingAddedNotification.class),
    SHERIFF_ORDER_FOR_POSTING_DELETED(SheriffOrderForPostingDeletedNotification.class),
    SHERIFF_ORDER_FOR_COMMENT_ADDED(SheriffOrderForCommentAddedNotification.class),
    SHERIFF_ORDER_FOR_COMMENT_DELETED(SheriffOrderForCommentDeletedNotification.class),
    SHERIFF_COMPLAINT_DECIDED(SheriffComplaintDecidedNotification.class),
    USER_LIST_ITEM_ADDED(UserListItemAddedNotification.class),
    USER_LIST_ITEM_DELETED(UserListItemDeletedNotification.class),
    GRANT_UPDATED(GrantUpdatedNotification.class);

    private final Class<? extends Notification> structure;

    NotificationType(Class<? extends Notification> structure) {
        this.structure = structure;
    }

    public Class<? extends Notification> getStructure() {
        return structure;
    }

    public String getValue() {
        return name().toLowerCase().replace('_', '-');
    }

    public static NotificationType forValue(String value) {
        String name = value.toUpperCase().replace('-', '_');
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
