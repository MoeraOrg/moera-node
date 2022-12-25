package org.moera.node.rest.notification;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.moera.node.data.Contact;
import org.moera.node.data.ContactRepository;
import org.moera.node.data.FriendOf;
import org.moera.node.data.FriendOfRepository;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.model.RemoteFriendGroupDeletedLiberin;
import org.moera.node.liberin.model.RemoteFriendshipUpdatedLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.model.AvatarImage;
import org.moera.node.model.FriendGroupDetails;
import org.moera.node.model.notification.FriendGroupDeletedNotification;
import org.moera.node.model.notification.FriendGroupUpdatedNotification;
import org.moera.node.model.notification.FriendshipUpdatedNotification;
import org.moera.node.model.notification.NotificationType;
import org.moera.node.notification.receive.NotificationMapping;
import org.moera.node.notification.receive.NotificationProcessor;
import org.moera.node.operations.ContactOperations;
import org.moera.node.util.Transaction;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;

@NotificationProcessor
public class FriendProcessor {

    private static final Logger log = LoggerFactory.getLogger(FriendProcessor.class);

    @Inject
    private UniversalContext universalContext;

    @Inject
    private FriendOfRepository friendOfRepository;

    @Inject
    private ContactRepository contactRepository;

    @Inject
    private MediaManager mediaManager;

    @Inject
    private ContactOperations contactOperations;

    @Inject
    private PlatformTransactionManager txManager;

    @NotificationMapping(NotificationType.FRIENDSHIP_UPDATED)
    @Transactional
    public void friendshipUpdated(FriendshipUpdatedNotification notification) {
        Map<String, FriendOf> previous = friendOfRepository.findByNodeIdAndRemoteNode(
                universalContext.nodeId(), notification.getSenderNodeName()).stream()
                .collect(Collectors.toMap(FriendOf::getRemoteGroupId, Function.identity()));
        Map<String, FriendGroupDetails> current = Arrays.stream(notification.getFriendGroups())
                .collect(Collectors.toMap(FriendGroupDetails::getId, Function.identity()));

        RemoteFriendshipUpdatedLiberin liberin = new RemoteFriendshipUpdatedLiberin();
        Contact contact = contactOperations.find(notification.getSenderNodeName());

        for (var prev : previous.entrySet()) {
            if (!current.containsKey(prev.getKey())) {
                friendOfRepository.delete(prev.getValue());
                contactOperations.updateFriendOfCount(notification.getSenderNodeName(), -1);
                liberin.getDeleted().add(prev.getValue());
            }
        }

        for (var curr : current.entrySet()) {
            FriendOf friendOf = previous.get(curr.getKey());
            if (friendOf == null) {
                friendOf = new FriendOf();
                friendOf.setId(UUID.randomUUID());
                friendOf.setNodeId(universalContext.nodeId());
                friendOf.setRemoteNodeName(notification.getSenderNodeName());
                friendOf.setContact(contact);
                friendOf.setRemoteGroupId(curr.getValue().getId());
                friendOf.setRemoteAddedAt(Util.toTimestamp(curr.getValue().getAddedAt()));
                friendOf = friendOfRepository.save(friendOf);
                liberin.getAdded().add(friendOf);
            }
            friendOf.setRemoteGroupTitle(curr.getValue().getTitle());
            liberin.getCurrent().add(friendOf);
        }

        Contact updatedContact = contactOperations.updateFriendOfCount(
                notification.getSenderNodeName(), liberin.getAdded().size());
        Contact.toAvatar(updatedContact, notification.getSenderAvatar());

        mediaManager.asyncDownloadPublicMedia(notification.getSenderNodeName(),
                new AvatarImage[] {notification.getSenderAvatar()},
                () -> updateAvatarsAndSend(notification.getSenderAvatar(), updatedContact, liberin));
    }

    private void updateAvatarsAndSend(AvatarImage avatarImage, Contact contact, RemoteFriendshipUpdatedLiberin liberin) {
        try {
            Transaction.execute(txManager, () -> {
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
                liberin.setContact(contact);
                universalContext.send(liberin);
                return null;
            });
        } catch (Throwable e) {
            log.error("Error saving the downloaded avatar: {}", e.getMessage());
        }
    }

    @NotificationMapping(NotificationType.FRIEND_GROUP_UPDATED)
    @Transactional
    public void friendGroupUpdated(FriendGroupUpdatedNotification notification) {
        FriendOf friendOf = friendOfRepository.findByNodeIdAndRemoteGroup(universalContext.nodeId(),
                notification.getSenderNodeName(), notification.getFriendGroup().getId()).orElse(null);
        if (friendOf == null) {
            return;
        }
        String prevTitle = friendOf.getRemoteGroupTitle();
        friendOf.setRemoteGroupTitle(notification.getFriendGroup().getTitle());
        if (prevTitle == null && friendOf.getRemoteGroupTitle() != null) {
            RemoteFriendshipUpdatedLiberin liberin = new RemoteFriendshipUpdatedLiberin();
            liberin.getAdded().add(friendOf);
            friendOfRepository.findByNodeIdAndRemoteNode(
                    universalContext.nodeId(), notification.getSenderNodeName()).stream()
                    .map(fo -> fo.getRemoteGroupId().equals(friendOf.getRemoteGroupId()) ? friendOf : fo)
                    .forEach(liberin.getCurrent()::add);
            universalContext.send(liberin);
        } else if (prevTitle != null && friendOf.getRemoteGroupTitle() == null) {
            universalContext.send(new RemoteFriendGroupDeletedLiberin(friendOf));
        }
    }

    @NotificationMapping(NotificationType.FRIEND_GROUP_DELETED)
    @Transactional
    public void friendGroupDeleted(FriendGroupDeletedNotification notification) {
        friendOfRepository.findByNodeIdAndRemoteGroup(
                universalContext.nodeId(),
                notification.getSenderNodeName(),
                notification.getFriendGroupId()
        ).ifPresent(friendOf -> universalContext.send(new RemoteFriendGroupDeletedLiberin(friendOf)));
    }

}
