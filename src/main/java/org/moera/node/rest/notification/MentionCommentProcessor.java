package org.moera.node.rest.notification;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.model.MentionInRemoteCommentAddedLiberin;
import org.moera.node.liberin.model.MentionInRemoteCommentDeletedLiberin;
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
    private UniversalContext universalContext;

    @Inject
    private MediaManager mediaManager;

    @NotificationMapping(NotificationType.MENTION_COMMENT_ADDED)
    @Transactional
    public void added(MentionCommentAddedNotification notification) {
        mediaManager.asyncDownloadPublicMedia(notification.getSenderNodeName(),
                new AvatarImage[] {notification.getPostingOwnerAvatar(), notification.getCommentOwnerAvatar()},
                mediaFiles -> {
                    if (notification.getPostingOwnerAvatar() != null) {
                        notification.getPostingOwnerAvatar().setMediaFile(mediaFiles[0]);
                    }
                    if (notification.getCommentOwnerAvatar() != null) {
                        notification.getCommentOwnerAvatar().setMediaFile(mediaFiles[1]);
                    }
                    universalContext.send(
                            new MentionInRemoteCommentAddedLiberin(notification.getSenderNodeName(),
                                    notification.getPostingOwnerName(), notification.getPostingOwnerFullName(),
                                    notification.getPostingOwnerAvatar(), notification.getPostingId(),
                                    notification.getPostingHeading(), notification.getCommentOwnerName(),
                                    notification.getCommentOwnerFullName(), notification.getCommentOwnerAvatar(),
                                    notification.getCommentId(), notification.getCommentHeading()));
                });
    }

    @NotificationMapping(NotificationType.MENTION_COMMENT_DELETED)
    @Transactional
    public void deleted(MentionCommentDeletedNotification notification) {
        universalContext.send(
                new MentionInRemoteCommentDeletedLiberin(notification.getSenderNodeName(), notification.getPostingId(),
                        notification.getCommentId()));
    }

}
