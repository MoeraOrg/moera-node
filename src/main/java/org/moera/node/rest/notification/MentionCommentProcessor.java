package org.moera.node.rest.notification;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.moera.node.instant.MentionCommentInstants;
import org.moera.node.media.MediaManager;
import org.moera.node.model.AvatarImage;
import org.moera.node.model.notification.MentionCommentAddedNotification;
import org.moera.node.model.notification.MentionCommentDeletedNotification;
import org.moera.node.model.notification.NotificationType;
import org.moera.node.notification.receive.NotificationMapping;
import org.moera.node.notification.receive.NotificationProcessor;

@NotificationProcessor
public class MentionCommentProcessor {

    @Inject
    private MentionCommentInstants mentionCommentInstants;

    @Inject
    private MediaManager mediaManager;

    @NotificationMapping(NotificationType.MENTION_COMMENT_ADDED)
    @Transactional
    public void added(MentionCommentAddedNotification notification) {
        mediaManager.asyncDownloadPublicMedia(notification.getSenderNodeName(),
                new AvatarImage[] {notification.getSenderAvatar(), notification.getCommentOwnerAvatar()},
                mediaFiles -> {
                    notification.getSenderAvatar().setMediaFile(mediaFiles[0]);
                    notification.getCommentOwnerAvatar().setMediaFile(mediaFiles[1]);
                    mentionCommentInstants.added(notification.getSenderNodeName(), notification.getSenderFullName(),
                            notification.getSenderAvatar(), notification.getPostingId(),
                            notification.getPostingHeading(), notification.getCommentOwnerName(),
                            notification.getCommentOwnerFullName(), notification.getCommentOwnerAvatar(),
                            notification.getCommentId(), notification.getCommentHeading());
                });
    }

    @NotificationMapping(NotificationType.MENTION_COMMENT_DELETED)
    @Transactional
    public void deleted(MentionCommentDeletedNotification notification) {
        mentionCommentInstants.deleted(notification.getSenderNodeName(), notification.getPostingId(),
                notification.getCommentId());
    }

}
