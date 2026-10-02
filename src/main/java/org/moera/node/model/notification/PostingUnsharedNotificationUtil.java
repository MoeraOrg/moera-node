package org.moera.node.model.notification;

import java.util.UUID;

import org.moera.lib.node.types.SharingType;
import org.moera.lib.node.types.notifications.PostingUnsharedNotification;

public class PostingUnsharedNotificationUtil {

    public static PostingUnsharedNotification build(
        SharingType sharingType,
        String postingId,
        String postingRevisionId,
        String remoteFeedName,
        UUID remoteStoryId,
        Long remoteStoryMoment,
        UUID remotePostingId
    ) {
        PostingUnsharedNotification notification = new PostingUnsharedNotification();
        notification.setSharingType(sharingType);
        notification.setPostingId(postingId);
        notification.setPostingRevisionId(postingRevisionId);
        notification.setRemoteFeedName(remoteFeedName);
        notification.setRemoteStoryId(remoteStoryId != null ? remoteStoryId.toString() : null);
        notification.setRemoteStoryMoment(remoteStoryMoment);
        notification.setRemotePostingId(remotePostingId != null ? remotePostingId.toString() : null);
        return notification;
    }

}
