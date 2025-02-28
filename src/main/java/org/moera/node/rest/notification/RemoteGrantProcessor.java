package org.moera.node.rest.notification;

import jakarta.inject.Inject;

import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.notifications.GrantUpdatedNotification;
import org.moera.lib.node.types.notifications.NotificationType;
import org.moera.node.notification.receive.NotificationMapping;
import org.moera.node.notification.receive.NotificationProcessor;
import org.moera.node.operations.RemoteGrantOperations;
import org.moera.node.util.Transaction;

@NotificationProcessor
public class RemoteGrantProcessor {

    @Inject
    private RemoteGrantOperations remoteGrantOperations;

    @Inject
    private Transaction tx;

    @NotificationMapping(NotificationType.GRANT_UPDATED)
    public void asked(GrantUpdatedNotification notification) {
        tx.executeWrite(() -> {
            remoteGrantOperations.put(notification.getSenderNodeName(), Scope.forValues(notification.getScope()));
        });
    }

}
