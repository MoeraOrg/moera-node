package org.moera.node.rest.notification;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.model.RemoteCommentReactionAddedLiberin;
import org.moera.node.liberin.model.RemoteCommentReactionDeletedAllLiberin;
import org.moera.node.liberin.model.RemoteCommentReactionDeletedLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.model.AvatarImage;
import org.moera.node.model.notification.CommentReactionAddedNotification;
import org.moera.node.model.notification.CommentReactionDeletedAllNotification;
import org.moera.node.model.notification.CommentReactionDeletedNotification;
import org.moera.node.model.notification.NotificationType;
import org.moera.node.notification.receive.NotificationMapping;
import org.moera.node.notification.receive.NotificationProcessor;

@NotificationProcessor
public class CommentReactionProcessor {

    @Inject
    private UniversalContext universalContext;

    @Inject
    private MediaManager mediaManager;

    @NotificationMapping(NotificationType.COMMENT_REACTION_ADDED)
    @Transactional
    public void added(CommentReactionAddedNotification notification) {
        mediaManager.asyncDownloadPublicMedia(notification.getSenderNodeName(),
                new AvatarImage[] {notification.getPostingAvatar(), notification.getOwnerAvatar()},
                mediaFiles -> {
                    if (notification.getPostingAvatar() != null) {
                        notification.getPostingAvatar().setMediaFile(mediaFiles[0]);
                    }
                    if (notification.getOwnerAvatar() != null) {
                        notification.getOwnerAvatar().setMediaFile(mediaFiles[1]);
                    }
                    universalContext.send(new RemoteCommentReactionAddedLiberin(notification.getSenderNodeName(),
                            notification.getPostingNodeName(), notification.getPostingFullName(),
                            notification.getPostingGender(), notification.getPostingAvatar(),
                            notification.getPostingId(), notification.getCommentId(), notification.getOwnerName(),
                            notification.getOwnerFullName(), notification.getOwnerGender(),
                            notification.getOwnerAvatar(), notification.getCommentHeading(), notification.isNegative(),
                            notification.getEmoji()));
                });
    }

    @NotificationMapping(NotificationType.COMMENT_REACTION_DELETED)
    @Transactional
    public void deleted(CommentReactionDeletedNotification notification) {
        universalContext.send(new RemoteCommentReactionDeletedLiberin(notification.getSenderNodeName(),
                notification.getPostingId(), notification.getCommentId(), notification.getOwnerName(),
                notification.isNegative()));
    }

    @NotificationMapping(NotificationType.COMMENT_REACTION_DELETED_ALL)
    @Transactional
    public void deletedAll(CommentReactionDeletedAllNotification notification) {
        universalContext.send(new RemoteCommentReactionDeletedAllLiberin(notification.getSenderNodeName(),
                notification.getPostingId(), notification.getCommentId()));
    }

}
