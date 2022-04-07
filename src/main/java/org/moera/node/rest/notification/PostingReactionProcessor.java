package org.moera.node.rest.notification;

import java.util.UUID;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.global.UniversalContext;
import org.moera.node.instant.PostingMediaReactionInstants;
import org.moera.node.instant.PostingReactionInstants;
import org.moera.node.liberin.model.RemoteCommentMediaReactionAddedLiberin;
import org.moera.node.liberin.model.RemoteCommentMediaReactionDeletedAllLiberin;
import org.moera.node.liberin.model.RemoteCommentMediaReactionDeletedLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.model.AvatarImage;
import org.moera.node.model.notification.NotificationType;
import org.moera.node.model.notification.PostingReactionAddedNotification;
import org.moera.node.model.notification.PostingReactionDeletedAllNotification;
import org.moera.node.model.notification.PostingReactionDeletedNotification;
import org.moera.node.notification.receive.NotificationMapping;
import org.moera.node.notification.receive.NotificationProcessor;

@NotificationProcessor
public class PostingReactionProcessor {

    @Inject
    private UniversalContext universalContext;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private PostingReactionInstants postingReactionInstants;

    @Inject
    private PostingMediaReactionInstants postingMediaReactionInstants;

    @Inject
    private MediaManager mediaManager;

    @NotificationMapping(NotificationType.POSTING_REACTION_ADDED)
    @Transactional
    public void added(PostingReactionAddedNotification notification) {
        mediaManager.asyncDownloadPublicMedia(notification.getSenderNodeName(),
                new AvatarImage[] {notification.getSenderAvatar(), notification.getOwnerAvatar()},
                mediaFiles -> {
                    if (notification.getSenderAvatar() != null) {
                        notification.getSenderAvatar().setMediaFile(mediaFiles[0]);
                    }
                    if (notification.getOwnerAvatar() != null) {
                        notification.getOwnerAvatar().setMediaFile(mediaFiles[1]);
                    }
                    if (notification.getParentPostingId() == null) {
                        if (notification.getSenderNodeName().equals(universalContext.nodeName())) {
                            addedToLocalPosting(notification);
                        } else {
                            // TODO
                        }
                    } else {
                        if (notification.getParentCommentId() == null) {
                            addedToPostingMedia(notification);
                        } else {
                            addedToCommentMedia(notification);
                        }
                    }
                });
    }

    private void addedToLocalPosting(PostingReactionAddedNotification notification) {
        Posting posting = postingRepository.findByNodeIdAndId(universalContext.nodeId(),
                UUID.fromString(notification.getPostingId())).orElse(null);
        if (posting == null) {
            return;
        }
        postingReactionInstants.added(posting, notification.getOwnerName(), notification.getOwnerFullName(),
                notification.getOwnerAvatar(), notification.isNegative(), notification.getEmoji());
    }

    private void addedToPostingMedia(PostingReactionAddedNotification notification) {
        postingMediaReactionInstants.added(notification.getSenderNodeName(), notification.getSenderFullName(),
                notification.getSenderAvatar(), notification.getPostingId(), notification.getParentPostingId(),
                notification.getParentMediaId(), notification.getOwnerName(), notification.getOwnerFullName(),
                notification.getOwnerAvatar(), notification.getPostingHeading(), notification.isNegative(),
                notification.getEmoji());
    }

    private void addedToCommentMedia(PostingReactionAddedNotification notification) {
        universalContext.send(
                new RemoteCommentMediaReactionAddedLiberin(notification.getSenderNodeName(),
                        notification.getSenderFullName(), notification.getSenderAvatar(), notification.getPostingId(),
                        notification.getParentPostingId(), notification.getParentCommentId(),
                        notification.getParentMediaId(), notification.getOwnerName(), notification.getOwnerFullName(),
                        notification.getOwnerAvatar(), notification.getPostingHeading(), notification.isNegative(),
                        notification.getEmoji()));
    }

    @NotificationMapping(NotificationType.POSTING_REACTION_DELETED)
    @Transactional
    public void deleted(PostingReactionDeletedNotification notification) {
        if (notification.getParentPostingId() == null) {
            if (notification.getSenderNodeName().equals(universalContext.nodeName())) {
                postingReactionInstants.deleted(UUID.fromString(notification.getPostingId()),
                        notification.getOwnerName(), notification.isNegative());
            } else {
                // TODO
            }
        } else {
            if (notification.getParentCommentId() == null) {
                postingMediaReactionInstants.deleted(notification.getSenderNodeName(), notification.getPostingId(),
                        notification.getOwnerName(), notification.isNegative());
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
        if (notification.getParentPostingId() == null) {
            if (notification.getSenderNodeName().equals(universalContext.nodeName())) {
                postingReactionInstants.deletedAll(UUID.fromString(notification.getPostingId()));
            } else {
                // TODO
            }
        } else {
            if (notification.getParentCommentId() == null) {
                postingMediaReactionInstants.deletedAll(notification.getSenderNodeName(), notification.getPostingId());
            } else {
                universalContext.send(
                        new RemoteCommentMediaReactionDeletedAllLiberin(notification.getSenderNodeName(),
                                notification.getPostingId()));
            }
        }
    }

}
