package org.moera.node.rest.notification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.moera.node.data.Contact;
import org.moera.node.data.FriendOf;
import org.moera.node.data.FriendOfRepository;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.model.RemoteFriendGroupDeletedLiberin;
import org.moera.node.liberin.model.RemoteFromFriendGroupDeletedLiberin;
import org.moera.node.liberin.model.RemoteToFriendGroupAddedLiberin;
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

        for (var prev : previous.entrySet()) {
            if (!current.containsKey(prev.getKey())) {
                friendOfRepository.delete(prev.getValue());
                universalContext.send(new RemoteFromFriendGroupDeletedLiberin(prev.getValue()));
            }
        }

        List<RemoteToFriendGroupAddedLiberin> added = new ArrayList<>();
        for (var curr : current.entrySet()) {
            FriendOf friendOf = previous.get(curr.getKey());
            if (friendOf == null) {
                friendOf = new FriendOf();
                friendOf.setId(UUID.randomUUID());
                friendOf.setNodeId(universalContext.nodeId());
                friendOf.setRemoteNodeName(notification.getSenderNodeName());
                friendOf.setRemoteGroupId(curr.getValue().getId());
                friendOf.setRemoteAddedAt(Util.toTimestamp(curr.getValue().getAddedAt()));
                friendOf = friendOfRepository.save(friendOf);
                added.add(new RemoteToFriendGroupAddedLiberin(friendOf));
            }
            friendOf.setRemoteFullName(notification.getSenderFullName());
            friendOf.setRemoteGender(notification.getSenderGender());
            friendOf.setRemoteGroupTitle(curr.getValue().getTitle());
        }

        if (added.isEmpty()) {
            return;
        }

        Contact.toAvatar(
                contactOperations.updateCloseness(notification.getSenderNodeName(), 1),
                notification.getSenderAvatar());

        mediaManager.asyncDownloadPublicMedia(notification.getSenderNodeName(),
                new AvatarImage[] {notification.getSenderAvatar()},
                () -> updateAvatarsAndSend(notification.getSenderAvatar(), added));
    }

    private void updateAvatarsAndSend(AvatarImage avatarImage, List<RemoteToFriendGroupAddedLiberin> liberins) {
        try {
            Transaction.execute(txManager, () -> {
                if (avatarImage != null) {
                    friendOfRepository.updateRemoteAvatar(
                            universalContext.nodeId(),
                            liberins.get(0).getFriendOf().getRemoteNodeName(),
                            avatarImage.getMediaFile(),
                            avatarImage.getShape()
                    );
                }
                for (var liberin : liberins) {
                    if (avatarImage != null) {
                        liberin.getFriendOf().setRemoteAvatarMediaFile(avatarImage.getMediaFile());
                        liberin.getFriendOf().setRemoteAvatarShape(avatarImage.getShape());
                    }
                    universalContext.send(liberin);
                }
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
            universalContext.send(new RemoteToFriendGroupAddedLiberin(friendOf));
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
