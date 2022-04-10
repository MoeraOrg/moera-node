package org.moera.node.rest.notification;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.model.ReplyCommentAddedLiberin;
import org.moera.node.liberin.model.ReplyCommentDeletedLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.model.AvatarImage;
import org.moera.node.model.notification.NotificationType;
import org.moera.node.model.notification.ReplyCommentAddedNotification;
import org.moera.node.model.notification.ReplyCommentDeletedNotification;
import org.moera.node.notification.receive.NotificationMapping;
import org.moera.node.notification.receive.NotificationProcessor;

@NotificationProcessor
public class ReplyCommentProcessor {

    @Inject
    private UniversalContext universalContext;

    @Inject
    private MediaManager mediaManager;

    @NotificationMapping(NotificationType.REPLY_COMMENT_ADDED)
    @Transactional
    public void added(ReplyCommentAddedNotification notification) {
        mediaManager.asyncDownloadPublicMedia(notification.getSenderNodeName(),
                new AvatarImage[] {notification.getSenderAvatar(), notification.getCommentOwnerAvatar()},
                mediaFiles -> {
                    if (notification.getSenderAvatar() != null) {
                        notification.getSenderAvatar().setMediaFile(mediaFiles[0]);
                    }
                    if (notification.getCommentOwnerAvatar() != null) {
                        notification.getCommentOwnerAvatar().setMediaFile(mediaFiles[1]);
                    }
                    universalContext.send(
                            new ReplyCommentAddedLiberin(notification.getSenderNodeName(),
                                    notification.getSenderFullName(), notification.getSenderAvatar(),
                                    notification.getPostingId(), notification.getPostingHeading(),
                                    notification.getCommentId(), notification.getRepliedToId(),
                                    notification.getRepliedToHeading(), notification.getCommentOwnerName(),
                                    notification.getCommentOwnerFullName(), notification.getCommentOwnerAvatar()));
                });
    }

    @NotificationMapping(NotificationType.REPLY_COMMENT_DELETED)
    @Transactional
    public void deleted(ReplyCommentDeletedNotification notification) {
        universalContext.send(
                new ReplyCommentDeletedLiberin(notification.getSenderNodeName(), notification.getPostingId(),
                        notification.getCommentId(), notification.getCommentOwnerName()));
    }

}
