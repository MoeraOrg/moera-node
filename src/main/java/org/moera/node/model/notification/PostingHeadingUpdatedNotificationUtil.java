package org.moera.node.model.notification;

import java.util.UUID;

import org.moera.lib.node.types.notifications.PostingHeadingUpdatedNotification;

public class PostingHeadingUpdatedNotificationUtil {

    public static PostingHeadingUpdatedNotification build(
        UUID postingId, UUID revisionId, String heading, String description
    ) {
        PostingHeadingUpdatedNotification notification = new PostingHeadingUpdatedNotification();
        notification.setPostingId(postingId.toString());
        notification.setRevisionId(revisionId.toString());
        notification.setHeading(heading);
        notification.setDescription(description);
        return notification;
    }

}
