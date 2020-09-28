package org.moera.node.model.notification;

public enum NotificationType {

    MENTION_POSTING_ADDED(MentionPostingAddedNotification.class),
    MENTION_POSTING_DELETED(MentionPostingDeletedNotification.class),
    FEED_POSTING_ADDED(FeedPostingAddedNotification.class),
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
    POSTING_COMMENT_ADDED(PostingCommentAddedNotification.class);

    private Class<? extends Notification> structure;

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
