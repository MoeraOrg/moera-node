package org.moera.node.rest.notification;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.moera.node.auth.AuthenticationException;
import org.moera.node.data.AskHistory;
import org.moera.node.data.AskHistoryRepository;
import org.moera.node.data.FriendGroup;
import org.moera.node.data.FriendGroupRepository;
import org.moera.node.data.SubscriptionType;
import org.moera.node.data.UserSubscriptionRepository;
import org.moera.node.global.RequestContext;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.model.AskedToFriendLiberin;
import org.moera.node.liberin.model.AskedToSubscribeLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.model.AskSubject;
import org.moera.node.model.AvatarImage;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.ValidationFailure;
import org.moera.node.model.notification.AskedNotification;
import org.moera.node.model.notification.NotificationType;
import org.moera.node.notification.receive.NotificationMapping;
import org.moera.node.notification.receive.NotificationProcessor;

@NotificationProcessor
public class AskProcessor {

    @Inject
    private RequestContext requestContext;

    @Inject
    private UniversalContext universalContext;

    @Inject
    private AskHistoryRepository askHistoryRepository;

    @Inject
    private UserSubscriptionRepository userSubscriptionRepository;

    @Inject
    private FriendGroupRepository friendGroupRepository;

    @Inject
    private MediaManager mediaManager;

    @NotificationMapping(NotificationType.ASKED)
    @Transactional
    public void asked(AskedNotification notification) {
        int total = askHistoryRepository.countByRemoteNode(universalContext.nodeId(), notification.getSenderNodeName());
        if (total >= requestContext.getOptions().getInt("ask.total.max")) {
            throw new OperationFailure("ask.too-many");
        }
        Timestamp last = askHistoryRepository.findLastCreatedAt(universalContext.nodeId(),
                notification.getSenderNodeName(), notification.getSubject());
        if (last != null) {
            Duration askInterval = requestContext.getOptions().getDuration("ask.interval").getDuration();
            if (last.toInstant().plus(askInterval).isAfter(Instant.now())) {
                throw new OperationFailure("ask.too-often");
            }
        }

        switch (notification.getSubject()) {
            case SUBSCRIBE:
                if (!requestContext.isPrincipal(requestContext.getOptions().getPrincipal("ask.subscribe.allowed"))) {
                    throw new AuthenticationException();
                }

                int count = userSubscriptionRepository.countByTypeAndRemoteNode(universalContext.nodeId(),
                        SubscriptionType.FEED, notification.getSenderNodeName());
                if (count > 0) {
                    break;
                }

                saveToHistory(notification.getSenderNodeName(), notification.getSubject());

                mediaManager.asyncDownloadPublicMedia(notification.getSenderNodeName(),
                        new AvatarImage[] {notification.getSenderAvatar()},
                        mediaFiles -> {
                            if (notification.getSenderAvatar() != null) {
                                notification.getSenderAvatar().setMediaFile(mediaFiles[0]);
                            }
                            universalContext.send(new AskedToSubscribeLiberin(notification.getSenderNodeName(),
                                    notification.getSenderFullName(), notification.getSenderGender(),
                                    notification.getSenderAvatar(), notification.getMessage()));
                        });
                break;

            case FRIEND: {
                if (!requestContext.isPrincipal(requestContext.getOptions().getPrincipal("ask.friend.allowed"))) {
                    throw new AuthenticationException();
                }

                UUID friendGroupId;
                try {
                    friendGroupId = UUID.fromString(notification.getFriendGroupId());
                } catch (Exception e) {
                    throw new ValidationFailure("friend-group.not-found");
                }

                if (requestContext.isMemberOf(friendGroupId)) {
                    break;
                }

                FriendGroup friendGroup = friendGroupRepository
                        .findByNodeIdAndId(universalContext.nodeId(), friendGroupId)
                        .orElseThrow(() -> new ValidationFailure("friend-group.not-found"));
                if (!friendGroup.getViewPrincipal().isPublic()) {
                    throw new ValidationFailure("friend-group.not-found");
                }

                saveToHistory(notification.getSenderNodeName(), notification.getSubject());

                mediaManager.asyncDownloadPublicMedia(notification.getSenderNodeName(),
                        new AvatarImage[] {notification.getSenderAvatar()},
                        mediaFiles -> {
                            if (notification.getSenderAvatar() != null) {
                                notification.getSenderAvatar().setMediaFile(mediaFiles[0]);
                            }
                            universalContext.send(new AskedToFriendLiberin(notification.getSenderNodeName(),
                                    notification.getSenderFullName(), notification.getSenderGender(),
                                    notification.getSenderAvatar(), friendGroup.getId(), friendGroup.getTitle(),
                                    notification.getMessage()));
                        });
                break;
            }
        }
    }

    private void saveToHistory(String remoteNodeName, AskSubject subject) {
        AskHistory askHistory = new AskHistory();
        askHistory.setId(UUID.randomUUID());
        askHistory.setNodeId(universalContext.nodeId());
        askHistory.setRemoteNodeName(remoteNodeName);
        askHistory.setSubject(subject);
        askHistoryRepository.save(askHistory);
    }

}
