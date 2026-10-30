package org.moera.node.model.notification;

import java.util.List;
import java.util.UUID;

import org.moera.lib.node.types.SharingType;
import org.moera.lib.node.types.SheriffMark;
import org.moera.lib.node.types.notifications.PostingSharedNotification;

public class PostingSharedNotificationUtil {

    private static PostingSharedNotification build(
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

    public static PostingSharedNotification buildRepublish(
        String postingId,
        String postingRevisionId,
        String remoteFeedName,
        UUID remoteStoryId,
        Long remoteStoryMoment,
        UUID remotePostingId
    ) {
        return build(
            SharingType.REPUBLISH,
            postingId,
            postingRevisionId,
            remoteFeedName,
            remoteStoryId,
            remoteStoryMoment,
            remotePostingId
        );
    }

    public static PostingSharedNotification buildReference(
        String postingId,
        UUID remotePostingId,
        String remotePostingOwnerName,
        String remotePostingOwnerFullName,
        String remotePostingOwnerSourceUri,
        String remotePostingOwnerGender,
        String remotePostingHeading,
        List<String> remotePostingSheriffs,
        List<SheriffMark> remotePostingSheriffMarks
    ) {
        PostingSharedNotification notification = build(
            SharingType.REFER, postingId, null, null, null, null, remotePostingId
        );
        notification.setRemotePostingOwnerName(remotePostingOwnerName);
        notification.setRemotePostingOwnerFullName(remotePostingOwnerFullName);
        notification.setRemotePostingOwnerSourceUri(remotePostingOwnerSourceUri);
        notification.setRemotePostingOwnerGender(remotePostingOwnerGender);
        notification.setRemotePostingHeading(remotePostingHeading);
        notification.setRemotePostingSheriffs(remotePostingSheriffs);
        notification.setRemotePostingSheriffMarks(remotePostingSheriffMarks);
        return notification;
    }

}
