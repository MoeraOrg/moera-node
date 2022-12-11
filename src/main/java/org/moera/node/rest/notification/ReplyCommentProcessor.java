package org.moera.node.rest.notification;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.moera.node.data.Contact;
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
import org.moera.node.operations.ContactOperations;

@NotificationProcessor
public class ReplyCommentProcessor {

    @Inject
    private UniversalContext universalContext;

    @Inject
    private MediaManager mediaManager;

    @Inject
    private ContactOperations contactOperations;

    @NotificationMapping(NotificationType.REPLY_COMMENT_ADDED)
    @Transactional
    public void added(ReplyCommentAddedNotification notification) {
        Contact.toAvatar(
                contactOperations.updateCloseness(notification.getPostingOwnerName(), 0),
                notification.getPostingOwnerAvatar());
        Contact.toAvatar(
                contactOperations.updateCloseness(notification.getCommentOwnerName(), 0),
                notification.getCommentOwnerAvatar());
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
                            new ReplyCommentAddedLiberin(notification.getSenderNodeName(),
                                    notification.getPostingOwnerName(), notification.getPostingOwnerFullName(),
                                    notification.getPostingOwnerGender(), notification.getPostingOwnerAvatar(),
                                    notification.getPostingHeading(), notification.getPostingId(),
                                    notification.getRepliedToHeading(), notification.getRepliedToId(),
                                    notification.getCommentOwnerName(), notification.getCommentOwnerFullName(),
                                    notification.getCommentOwnerGender(), notification.getCommentOwnerAvatar(),
                                    notification.getCommentId()));
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
