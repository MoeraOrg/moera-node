package org.moera.node.rest.notification;

import java.util.Collection;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.moera.lib.node.types.notifications.BlockingAddedNotification;
import org.moera.lib.node.types.notifications.BlockingDeletedNotification;
import org.moera.lib.node.types.notifications.NotificationType;
import org.moera.node.data.BlockedByUser;
import org.moera.node.data.BlockedByUserRepository;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.model.BlockedByUserDeletedLiberin;
import org.moera.node.notification.receive.NotificationMapping;
import org.moera.node.notification.receive.NotificationProcessor;
import org.moera.node.operations.ContactOperations;
import org.moera.node.task.Jobs;

@NotificationProcessor
public class BlockingProcessor {

    @Inject
    private UniversalContext universalContext;

    @Inject
    private BlockedByUserRepository blockedByUserRepository;

    @Inject
    private ContactOperations contactOperations;

    @Inject
    private Jobs jobs;

    @NotificationMapping(NotificationType.BLOCKING_ADDED)
    public void added(BlockingAddedNotification notification) {
        jobs.run(
            BlockingAddedJob.class,
            new BlockingAddedJob.Parameters(
                notification.getSenderNodeName(),
                notification.getSenderFullName(),
                notification.getSenderGender(),
                notification.getSenderAvatar(),
                notification.getBlockedOperation(),
                notification.getPostingId(),
                notification.getPostingHeading(),
                notification.getDeadline(),
                notification.getReason()
            ),
            universalContext.nodeId()
        );
    }

    @NotificationMapping(NotificationType.BLOCKING_DELETED)
    @Transactional
    public void deleted(BlockingDeletedNotification notification) {
        Collection<BlockedByUser> blockedByUsers = notification.getPostingId() == null
            ? blockedByUserRepository.findByRemoteNode(
                universalContext.nodeId(), notification.getSenderNodeName()
            )
            : blockedByUserRepository.findByRemotePosting(
                universalContext.nodeId(), notification.getSenderNodeName(), notification.getPostingId()
            );
        BlockedByUser blockedByUser = blockedByUsers.stream()
            .filter(bbu -> bbu.getBlockedOperation() == notification.getBlockedOperation())
            .findFirst()
            .orElse(null);

        if (blockedByUser == null) {
            return;
        }

        blockedByUserRepository.delete(blockedByUser);
        contactOperations.updateBlockedByUserCounts(blockedByUser, -1).fill(blockedByUser);
        universalContext.send(new BlockedByUserDeletedLiberin(blockedByUser, notification.getPostingHeading()));
    }

}
