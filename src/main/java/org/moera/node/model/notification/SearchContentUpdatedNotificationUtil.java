package org.moera.node.model.notification;

import java.util.UUID;

import org.moera.lib.node.types.BlockedOperation;
import org.moera.lib.node.types.SearchBlockUpdate;
import org.moera.lib.node.types.SearchCommentUpdate;
import org.moera.lib.node.types.SearchContentUpdateType;
import org.moera.lib.node.types.SearchFriendUpdate;
import org.moera.lib.node.types.SearchPostingUpdate;
import org.moera.lib.node.types.SearchReactionUpdate;
import org.moera.lib.node.types.SearchSubscriptionUpdate;
import org.moera.lib.node.types.notifications.SearchContentUpdatedNotification;
import org.moera.node.data.Comment;
import org.moera.node.data.Posting;
import org.moera.node.data.Story;
import org.moera.node.util.Util;

public class SearchContentUpdatedNotificationUtil {

    public static SearchContentUpdatedNotification buildProfileUpdate() {
        var notification = new SearchContentUpdatedNotification();
        notification.setUpdateType(SearchContentUpdateType.PROFILE);
        return notification;
    }

    public static SearchContentUpdatedNotification buildFriendUpdate(
        SearchContentUpdateType updateType, String nodeName
    ) {
        var notification = new SearchContentUpdatedNotification();
        notification.setUpdateType(updateType);
        var details = new SearchFriendUpdate();
        details.setNodeName(nodeName);
        notification.setFriendUpdate(details);
        return notification;
    }

    public static SearchContentUpdatedNotification buildSubscriptionUpdate(
        SearchContentUpdateType updateType, String nodeName, String feedName
    ) {
        var notification = new SearchContentUpdatedNotification();
        notification.setUpdateType(updateType);
        var details = new SearchSubscriptionUpdate();
        details.setNodeName(nodeName);
        details.setFeedName(feedName);
        notification.setSubscriptionUpdate(details);
        return notification;
    }

    public static SearchContentUpdatedNotification buildBlockUpdate(
        SearchContentUpdateType updateType, String nodeName, BlockedOperation blockedOperation
    ) {
        var notification = new SearchContentUpdatedNotification();
        notification.setUpdateType(updateType);
        var details = new SearchBlockUpdate();
        details.setNodeName(nodeName);
        details.setBlockedOperation(blockedOperation);
        notification.setBlockUpdate(details);
        return notification;
    }

    public static SearchContentUpdatedNotification buildPostingUpdate(
        SearchContentUpdateType updateType, String nodeName, Story story
    ) {
        var notification = new SearchContentUpdatedNotification();
        notification.setUpdateType(updateType);
        var details = new SearchPostingUpdate();
        details.setFeedName(story.getFeedName());
        details.setStoryId(story.getId().toString());
        details.setPublishedAt(Util.toEpochSecond(story.getPublishedAt()));
        if (story.getEntry().isOriginal()) {
            details.setNodeName(nodeName);
            details.setPostingId(story.getEntry().getId().toString());
        } else {
            details.setNodeName(story.getEntry().getReceiverName());
            details.setPostingId(story.getEntry().getReceiverEntryId());
        }
        notification.setPostingUpdate(details);
        return notification;
    }

    public static SearchContentUpdatedNotification buildPostingUpdate(
        SearchContentUpdateType updateType, String nodeName, Posting posting
    ) {
        var notification = new SearchContentUpdatedNotification();
        notification.setUpdateType(updateType);
        var details = new SearchPostingUpdate();
        if (posting.isOriginal()) {
            details.setNodeName(nodeName);
            details.setPostingId(posting.getId().toString());
        } else {
            details.setNodeName(posting.getReceiverName());
            details.setPostingId(posting.getReceiverEntryId());
        }
        notification.setPostingUpdate(details);
        return notification;
    }

    public static SearchContentUpdatedNotification buildCommentUpdate(
        SearchContentUpdateType updateType, Comment comment
    ) {
        var notification = new SearchContentUpdatedNotification();
        notification.setUpdateType(updateType);
        var details = new SearchCommentUpdate();
        details.setPostingId(comment.getPosting().getId().toString());
        details.setCommentId(comment.getId().toString());
        notification.setCommentUpdate(details);
        return notification;
    }

    public static SearchContentUpdatedNotification buildReactionUpdate(
        SearchContentUpdateType updateType, UUID postingId, String ownerName
    ) {
        var notification = new SearchContentUpdatedNotification();
        notification.setUpdateType(updateType);
        var details = new SearchReactionUpdate();
        details.setPostingId(postingId.toString());
        details.setOwnerName(ownerName);
        notification.setReactionUpdate(details);
        return notification;
    }

    public static SearchContentUpdatedNotification buildReactionUpdate(
        SearchContentUpdateType updateType, UUID postingId, UUID commentId, String ownerName
    ) {
        var notification = new SearchContentUpdatedNotification();
        notification.setUpdateType(updateType);
        var details = new SearchReactionUpdate();
        details.setPostingId(postingId.toString());
        details.setCommentId(commentId.toString());
        details.setOwnerName(ownerName);
        notification.setReactionUpdate(details);
        return notification;
    }

}
