package org.moera.node.rest.notification;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.moera.lib.node.types.notifications.FriendGroupDeletedNotification;
import org.moera.lib.node.types.notifications.FriendGroupUpdatedNotification;
import org.moera.lib.node.types.notifications.FriendshipUpdatedNotification;
import org.moera.lib.node.types.notifications.NotificationType;
import org.moera.node.data.FriendOf;
import org.moera.node.data.FriendOfRepository;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.model.RemoteFriendGroupDeletedLiberin;
import org.moera.node.liberin.model.RemoteFriendshipUpdatedLiberin;
import org.moera.node.notification.receive.NotificationMapping;
import org.moera.node.notification.receive.NotificationProcessor;
import org.moera.node.task.Jobs;

@NotificationProcessor
public class FriendProcessor {

    @Inject
    private UniversalContext universalContext;

    @Inject
    private FriendOfRepository friendOfRepository;

    @Inject
    private Jobs jobs;

    @NotificationMapping(NotificationType.FRIENDSHIP_UPDATED)
    public void friendshipUpdated(FriendshipUpdatedNotification notification) {
        jobs.run(
            FriendshipUpdatedJob.class,
            new FriendshipUpdatedJob.Parameters(
                notification.getSenderNodeName(),
                notification.getSenderAvatar(),
                notification.getFriendGroups()
            ),
            universalContext.nodeId()
        );
    }

    @NotificationMapping(NotificationType.FRIEND_GROUP_UPDATED)
    @Transactional
    public void friendGroupUpdated(FriendGroupUpdatedNotification notification) {
        FriendOf friendOf = friendOfRepository.findByNodeIdAndRemoteGroup(
            universalContext.nodeId(), notification.getSenderNodeName(), notification.getFriendGroup().getId()
        ).orElse(null);
        if (friendOf == null) {
            return;
        }
        String prevTitle = friendOf.getRemoteGroupTitle();
        friendOf.setRemoteGroupTitle(notification.getFriendGroup().getTitle());
        if (prevTitle == null && friendOf.getRemoteGroupTitle() != null) {
            RemoteFriendshipUpdatedLiberin liberin = new RemoteFriendshipUpdatedLiberin();
            liberin.getAdded().add(friendOf);
            friendOfRepository.findByNodeIdAndRemoteNode(
                universalContext.nodeId(), notification.getSenderNodeName()
            )
                .stream()
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
