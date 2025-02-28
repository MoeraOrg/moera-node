package org.moera.node.model.notification;

import org.moera.lib.node.types.notifications.StoryAddedNotification;
import org.moera.node.data.Story;

public class StoryAddedNotificationUtil {
    
    public static StoryAddedNotification build(Story story) {
        StoryAddedNotification notification = new StoryAddedNotification();
        notification.setStoryId(story.getId().toString());
        notification.setFeedName(story.getFeedName());
        notification.setStoryType(story.getStoryType());
        if (story.getEntry() != null) {
            notification.setPostingId(story.getEntry().getId().toString());
        }
        return notification;
    }

}
