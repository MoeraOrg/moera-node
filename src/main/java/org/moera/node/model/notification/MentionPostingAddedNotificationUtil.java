package org.moera.node.model.notification;

import java.util.List;
import java.util.UUID;
import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.node.types.SheriffMark;
import org.moera.lib.node.types.notifications.MentionPostingAddedNotification;

public class MentionPostingAddedNotificationUtil {

    public static MentionPostingAddedNotification build(
        UUID postingId,
        String ownerName,
        String ownerFullName,
        String ownerGender,
        AvatarImage ownerAvatar,
        String heading,
        List<String> sheriffs,
        List<SheriffMark> sheriffMarks
    ) {
        MentionPostingAddedNotification notification = new MentionPostingAddedNotification();
        notification.setPostingId(postingId.toString());
        notification.setOwnerName(ownerName);
        notification.setOwnerFullName(ownerFullName);
        notification.setOwnerGender(ownerGender);
        notification.setOwnerAvatar(ownerAvatar);
        notification.setHeading(heading);
        notification.setSheriffs(sheriffs);
        notification.setSheriffMarks(sheriffMarks);
        return notification;
    }

}
