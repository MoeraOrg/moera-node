package org.moera.node.model.notification;

import java.util.UUID;

import org.moera.lib.node.types.SharingType;
import org.moera.lib.node.types.notifications.PostingSharedNotification;

public class PostingSharedNotificationUtil {

    public static PostingSharedNotification build(
        SharingType sharingType,
        String postingId,
        String postingRevisionId,
        String remoteFeedName,
        UUID remoteStoryId,
        Long remoteStoryMoment,
        UUID remotePostingId
    ) {
        PostingSharedNotification notification = new PostingSharedNotification();
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
