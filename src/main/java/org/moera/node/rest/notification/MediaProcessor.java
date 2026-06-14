package org.moera.node.rest.notification;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.moera.lib.node.types.notifications.LeasedMediaTitleUpdatedNotification;
import org.moera.lib.node.types.notifications.NotificationType;
import org.moera.node.data.EntryRevisionRepository;
import org.moera.node.data.RemoteMediaFileRepository;
import org.moera.node.global.RequestContext;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.notification.receive.NotificationMapping;
import org.moera.node.notification.receive.NotificationProcessor;

@NotificationProcessor
public class MediaProcessor {

    @Inject
    private RequestContext requestContext;

    @Inject
    private RemoteMediaFileRepository remoteMediaFileRepository;

    @Inject
    private EntryRevisionRepository entryRevisionRepository;

    @NotificationMapping(NotificationType.LEASED_MEDIA_TITLE_UPDATED)
    @Transactional
    public void leasedMediaTitleUpdated(LeasedMediaTitleUpdatedNotification notification) {
        int updated = remoteMediaFileRepository.updateTitleByMediaAndLease(
            requestContext.nodeId(),
            notification.getSenderNodeName(),
            notification.getMediaId(),
            notification.getLeaseId(),
            notification.getTitle()
        );
        if (updated == 0) {
            throw new ObjectNotFoundFailure("media-lease.not-found");
        }
        entryRevisionRepository.clearAttachmentsCacheByRemoteMedia(
            requestContext.nodeId(), notification.getSenderNodeName(), notification.getMediaId()
        );
    }

}
