package org.moera.node.rest.notification;

import jakarta.inject.Inject;

import org.moera.lib.node.types.notifications.NotificationType;
import org.moera.lib.node.types.notifications.PostingSharedNotification;
import org.moera.lib.node.types.notifications.PostingUnsharedNotification;
import org.moera.node.global.UniversalContext;
import org.moera.node.data.EntrySharing;
import org.moera.node.notification.receive.NotificationMapping;
import org.moera.node.notification.receive.NotificationProcessor;
import org.moera.node.operations.SharingOperations;
import org.moera.node.task.Jobs;
import org.moera.node.util.Transaction;

@NotificationProcessor
public class PostingSharingProcessor {

    @Inject
    private SharingOperations sharingOperations;

    @Inject
    private UniversalContext universalContext;

    @Inject
    private Jobs jobs;

    @Inject
    private Transaction tx;

    @NotificationMapping(NotificationType.POSTING_SHARED)
    public void shared(PostingSharedNotification notification) {
        SharingOperations.validate(
            notification.getSharingType(),
            notification.getPostingId(),
            notification.getPostingRevisionId(),
            notification.getRemoteFeedName(),
            notification.getRemoteStoryId(),
            notification.getRemoteStoryMoment(),
            notification.getRemotePostingId()
        );
        tx.executeWrite(() -> {
            EntrySharing sharing = sharingOperations.shared(notification);
            if (sharing != null) {
                jobs.runAfterCommit(
                    PostingSharedJob.class,
                    new PostingSharedJob.Parameters(
                        notification.getSenderNodeName(), notification.getSenderAvatar(),
                        sharing.getEntry().getId(), sharing.getId()
                    ),
                    universalContext.nodeId()
                );
            }
        });
    }

    @NotificationMapping(NotificationType.POSTING_UNSHARED)
    public void unshared(PostingUnsharedNotification notification) {
        SharingOperations.validate(
            notification.getSharingType(),
            notification.getPostingId(),
            notification.getPostingRevisionId(),
            notification.getRemoteFeedName(),
            notification.getRemoteStoryId(),
            notification.getRemoteStoryMoment(),
            notification.getRemotePostingId()
        );
        tx.executeWrite(() -> sharingOperations.unshared(notification));
    }

}
