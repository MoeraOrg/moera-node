package org.moera.node.model.notification;

import org.moera.lib.node.types.notifications.PostingSharingUpdatedNotification;
import org.moera.node.data.Story;

public class PostingSharingUpdatedNotificationUtil {

    public static PostingSharingUpdatedNotification build(Story story) {
        var notification = new PostingSharingUpdatedNotification();
        notification.setPostingId(story.getEntry().getReceiverEntryId());
        notification.setRemoteFeedName(story.getFeedName());
        notification.setRemoteStoryId(story.getId().toString());
        notification.setRemoteStoryMoment(story.getMoment());
        notification.setRemotePostingId(story.getEntry().getId().toString());
        return notification;
    }

}
