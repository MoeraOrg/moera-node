package org.moera.node.model.notification;

import java.util.UUID;

import org.moera.lib.node.types.notifications.PostingMediaTextUpdatedNotification;

public class PostingMediaTextUpdatedNotificationUtil {

    public static PostingMediaTextUpdatedNotification build(UUID postingId, UUID mediaId, String textContent) {
        PostingMediaTextUpdatedNotification notification = new PostingMediaTextUpdatedNotification();
        notification.setPostingId(postingId.toString());
        notification.setMediaId(mediaId.toString());
        notification.setTextContent(textContent);
        return notification;
    }

}
