package org.moera.node.rest.notification;

import javax.inject.Inject;

import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.model.RemoteSheriffOrderReceivedLiberin;
import org.moera.node.model.notification.NotificationType;
import org.moera.node.model.notification.SheriffOrderForCommentAddedNotification;
import org.moera.node.model.notification.SheriffOrderForCommentDeletedNotification;
import org.moera.node.model.notification.SheriffOrderForPostingAddedNotification;
import org.moera.node.model.notification.SheriffOrderForPostingDeletedNotification;
import org.moera.node.notification.receive.NotificationMapping;
import org.moera.node.notification.receive.NotificationProcessor;

@NotificationProcessor
public class SheriffProcessor {

    @Inject
    private UniversalContext universalContext;

    @NotificationMapping(NotificationType.SHERIFF_ORDER_FOR_POSTING_ADDED)
    public void orderForPostingAdded(SheriffOrderForPostingAddedNotification notification) {
        universalContext.send(new RemoteSheriffOrderReceivedLiberin(false, notification));
    }

    @NotificationMapping(NotificationType.SHERIFF_ORDER_FOR_POSTING_DELETED)
    public void orderForPostingDeleted(SheriffOrderForPostingDeletedNotification notification) {
        universalContext.send(new RemoteSheriffOrderReceivedLiberin(true, notification));
    }

    @NotificationMapping(NotificationType.SHERIFF_ORDER_FOR_COMMENT_ADDED)
    public void orderForCommentAdded(SheriffOrderForCommentAddedNotification notification) {
        universalContext.send(new RemoteSheriffOrderReceivedLiberin(false, notification));
    }

    @NotificationMapping(NotificationType.SHERIFF_ORDER_FOR_COMMENT_DELETED)
    public void orderForCommentDeleted(SheriffOrderForCommentDeletedNotification notification) {
        universalContext.send(new RemoteSheriffOrderReceivedLiberin(true, notification));
    }

}
