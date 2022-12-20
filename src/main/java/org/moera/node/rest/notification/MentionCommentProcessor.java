package org.moera.node.rest.notification;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.moera.node.data.Contact;
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
import org.moera.node.operations.ContactOperations;

@NotificationProcessor
public class MentionCommentProcessor {

    @Inject
    private UniversalContext universalContext;

    @Inject
    private MediaManager mediaManager;

    @Inject
    private ContactOperations contactOperations;

    @NotificationMapping(NotificationType.MENTION_COMMENT_ADDED)
    @Transactional
    public void added(MentionCommentAddedNotification notification) {
        Contact.toAvatar(
                contactOperations.find(notification.getPostingOwnerName()),
                notification.getPostingOwnerAvatar());
        Contact.toAvatar(
                contactOperations.find(notification.getCommentOwnerName()),
                notification.getCommentOwnerAvatar());

        mediaManager.asyncDownloadPublicMedia(notification.getSenderNodeName(),
                new AvatarImage[] {notification.getPostingOwnerAvatar(), notification.getCommentOwnerAvatar()},
                () -> universalContext.send(
                        new MentionInRemoteCommentAddedLiberin(notification.getSenderNodeName(),
                                notification.getPostingOwnerName(), notification.getPostingOwnerFullName(),
                                notification.getPostingOwnerGender(), notification.getPostingOwnerAvatar(),
                                notification.getPostingId(), notification.getPostingHeading(),
                                notification.getCommentOwnerName(), notification.getCommentOwnerFullName(),
                                notification.getCommentOwnerGender(), notification.getCommentOwnerAvatar(),
                                notification.getCommentId(), notification.getCommentHeading())));
    }

    @NotificationMapping(NotificationType.MENTION_COMMENT_DELETED)
    @Transactional
    public void deleted(MentionCommentDeletedNotification notification) {
        universalContext.send(
                new MentionInRemoteCommentDeletedLiberin(notification.getSenderNodeName(), notification.getPostingId(),
                        notification.getCommentId()));
    }

}
