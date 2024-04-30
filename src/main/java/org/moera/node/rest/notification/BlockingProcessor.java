package org.moera.node.rest.notification;

import java.util.Collection;
import java.util.UUID;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.moera.node.data.BlockedByUser;
import org.moera.node.data.BlockedByUserRepository;
import org.moera.node.data.Contact;
import org.moera.node.data.ContactRepository;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.model.BlockedByUserAddedLiberin;
import org.moera.node.liberin.model.BlockedByUserDeletedLiberin;
import org.moera.node.liberin.model.BlockedByUserLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.model.AvatarImage;
import org.moera.node.model.notification.BlockingAddedNotification;
import org.moera.node.model.notification.BlockingDeletedNotification;
import org.moera.node.model.notification.NotificationType;
import org.moera.node.notification.receive.NotificationMapping;
import org.moera.node.notification.receive.NotificationProcessor;
import org.moera.node.operations.ContactOperations;
import org.moera.node.util.Transaction;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NotificationProcessor
public class BlockingProcessor {

    private static final Logger log = LoggerFactory.getLogger(BlockingProcessor.class);

    @Inject
    private UniversalContext universalContext;

    @Inject
    private BlockedByUserRepository blockedByUserRepository;

    @Inject
    private ContactRepository contactRepository;

    @Inject
    private MediaManager mediaManager;

    @Inject
    private ContactOperations contactOperations;

    @Inject
    private Transaction tx;

    @NotificationMapping(NotificationType.BLOCKING_ADDED)
    @Transactional
    public void added(BlockingAddedNotification notification) {
        Contact contact = contactOperations.updateDetails(notification.getSenderNodeName(),
                notification.getSenderFullName(), notification.getSenderGender());

        BlockedByUser blockedByUser = new BlockedByUser();
        blockedByUser.setId(UUID.randomUUID());
        blockedByUser.setNodeId(universalContext.nodeId());
        blockedByUser.setBlockedOperation(notification.getBlockedOperation());
        blockedByUser.setContact(contact);
        blockedByUser.setRemoteNodeName(notification.getSenderNodeName());
        blockedByUser.setRemotePostingId(notification.getPostingId());
        blockedByUser.setDeadline(Util.toTimestamp(notification.getDeadline()));
        blockedByUser.setReason(notification.getReason() != null ? notification.getReason() : "");
        blockedByUser = blockedByUserRepository.save(blockedByUser);

        BlockedByUserAddedLiberin liberin =
                new BlockedByUserAddedLiberin(blockedByUser, notification.getPostingHeading());

        Contact updatedContact = contactOperations.updateBlockedByUserCounts(blockedByUser, 1);
        Contact.toAvatar(updatedContact, notification.getSenderAvatar());

        mediaManager.asyncDownloadPublicMedia(notification.getSenderNodeName(),
                new AvatarImage[] {notification.getSenderAvatar()},
                () -> updateAvatarsAndSend(notification.getSenderAvatar(), updatedContact, liberin));
    }

    private void updateAvatarsAndSend(AvatarImage avatarImage, Contact contact, BlockedByUserLiberin liberin) {
        tx.executeWriteQuietly(
            () -> {
                if (avatarImage != null) {
                    contactRepository.updateRemoteAvatar(
                            universalContext.nodeId(),
                            contact.getRemoteNodeName(),
                            avatarImage.getMediaFile(),
                            avatarImage.getShape()
                    );
                    contact.setRemoteAvatarMediaFile(avatarImage.getMediaFile());
                    contact.setRemoteAvatarShape(avatarImage.getShape());
                }
                liberin.getBlockedByUser().setContact(contact);
                universalContext.send(liberin);
            },
            e -> log.error("Error saving the downloaded avatar: {}", e.getMessage())
        );
    }

    @NotificationMapping(NotificationType.BLOCKING_DELETED)
    @Transactional
    public void deleted(BlockingDeletedNotification notification) {
        Collection<BlockedByUser> blockedByUsers = notification.getPostingId() == null
                ? blockedByUserRepository.findByRemoteNode(
                        universalContext.nodeId(), notification.getSenderNodeName())
                : blockedByUserRepository.findByRemotePosting(
                        universalContext.nodeId(), notification.getSenderNodeName(), notification.getPostingId());
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
