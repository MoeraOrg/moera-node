package org.moera.node.rest.notification;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.moera.node.data.Contact;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.model.RemoteCommentMediaReactionAddedLiberin;
import org.moera.node.liberin.model.RemoteCommentMediaReactionDeletedAllLiberin;
import org.moera.node.liberin.model.RemoteCommentMediaReactionDeletedLiberin;
import org.moera.node.liberin.model.RemotePostingMediaReactionAddedLiberin;
import org.moera.node.liberin.model.RemotePostingMediaReactionDeletedAllLiberin;
import org.moera.node.liberin.model.RemotePostingMediaReactionDeletedLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.model.AvatarImage;
import org.moera.node.model.notification.NotificationType;
import org.moera.node.model.notification.PostingReactionAddedNotification;
import org.moera.node.model.notification.PostingReactionDeletedAllNotification;
import org.moera.node.model.notification.PostingReactionDeletedNotification;
import org.moera.node.notification.receive.NotificationMapping;
import org.moera.node.notification.receive.NotificationProcessor;
import org.moera.node.operations.ContactOperations;

@NotificationProcessor
public class PostingReactionProcessor {

    @Inject
    private UniversalContext universalContext;

    @Inject
    private MediaManager mediaManager;

    @Inject
    private ContactOperations contactOperations;

    @NotificationMapping(NotificationType.POSTING_REACTION_ADDED)
    @Transactional
    public void added(PostingReactionAddedNotification notification) {
        Contact.toAvatar(
                contactOperations.find(notification.getParentPostingNodeName()),
                notification.getParentPostingAvatar());
        Contact.toAvatar(
                contactOperations.find(notification.getOwnerName()),
                notification.getOwnerAvatar());

        mediaManager.asyncDownloadPublicMedia(notification.getSenderNodeName(),
                new AvatarImage[] {notification.getParentPostingAvatar(), notification.getOwnerAvatar()},
                () -> {
                    if (notification.getParentPostingId() != null) {
                        if (notification.getParentCommentId() == null) {
                            addedToPostingMedia(notification);
                        } else {
                            addedToCommentMedia(notification);
                        }
                    }
                });
    }

    private void addedToPostingMedia(PostingReactionAddedNotification notification) {
        universalContext.send(
                new RemotePostingMediaReactionAddedLiberin(notification.getSenderNodeName(),
                        notification.getParentPostingNodeName(), notification.getParentPostingFullName(),
                        notification.getParentPostingGender(), notification.getParentPostingAvatar(),
                        notification.getPostingId(), notification.getParentPostingId(), notification.getParentMediaId(),
                        notification.getOwnerName(), notification.getOwnerFullName(), notification.getOwnerGender(),
                        notification.getOwnerAvatar(), notification.getParentHeading(), notification.isNegative(),
                        notification.getEmoji()));
    }

    private void addedToCommentMedia(PostingReactionAddedNotification notification) {
        universalContext.send(
                new RemoteCommentMediaReactionAddedLiberin(notification.getSenderNodeName(),
                        notification.getParentPostingNodeName(), notification.getParentPostingFullName(),
                        notification.getParentPostingGender(), notification.getParentPostingAvatar(),
                        notification.getPostingId(), notification.getParentPostingId(),
                        notification.getParentCommentId(), notification.getParentMediaId(), notification.getOwnerName(),
                        notification.getOwnerFullName(), notification.getOwnerGender(), notification.getOwnerAvatar(),
                        notification.getParentHeading(), notification.isNegative(), notification.getEmoji()));
    }

    @NotificationMapping(NotificationType.POSTING_REACTION_DELETED)
    @Transactional
    public void deleted(PostingReactionDeletedNotification notification) {
        if (notification.getParentPostingId() != null) {
            if (notification.getParentCommentId() == null) {
                universalContext.send(
                        new RemotePostingMediaReactionDeletedLiberin(notification.getSenderNodeName(),
                                notification.getPostingId(), notification.getOwnerName(), notification.isNegative()));
            } else {
                universalContext.send(
                        new RemoteCommentMediaReactionDeletedLiberin(notification.getSenderNodeName(),
                                notification.getPostingId(), notification.getOwnerName(), notification.isNegative()));
            }
        }
    }

    @NotificationMapping(NotificationType.POSTING_REACTION_DELETED_ALL)
    @Transactional
    public void deletedAll(PostingReactionDeletedAllNotification notification) {
        if (notification.getParentPostingId() != null) {
            if (notification.getParentCommentId() == null) {
                universalContext.send(
                        new RemotePostingMediaReactionDeletedAllLiberin(notification.getSenderNodeName(),
                                notification.getPostingId()));
            } else {
                universalContext.send(
                        new RemoteCommentMediaReactionDeletedAllLiberin(notification.getSenderNodeName(),
                                notification.getPostingId()));
            }
        }
    }

}
