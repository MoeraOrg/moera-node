package org.moera.node.rest.notification;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import javax.inject.Inject;

import org.moera.node.auth.AuthenticationException;
import org.moera.node.data.AskHistoryRepository;
import org.moera.node.data.FriendGroup;
import org.moera.node.data.FriendGroupRepository;
import org.moera.node.global.RequestContext;
import org.moera.node.global.UniversalContext;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.ValidationFailure;
import org.moera.node.model.notification.AskedNotification;
import org.moera.node.model.notification.NotificationType;
import org.moera.node.notification.receive.NotificationMapping;
import org.moera.node.notification.receive.NotificationProcessor;
import org.moera.node.task.Jobs;
import org.moera.node.util.Transaction;

@NotificationProcessor
public class AskProcessor {

    @Inject
    private RequestContext requestContext;

    @Inject
    private UniversalContext universalContext;

    @Inject
    private AskHistoryRepository askHistoryRepository;

    @Inject
    private FriendGroupRepository friendGroupRepository;

    @Inject
    private Transaction tx;

    @Inject
    private Jobs jobs;

    @NotificationMapping(NotificationType.ASKED)
    public void asked(AskedNotification notification) {
        tx.executeRead(() -> {
            int total = askHistoryRepository.countByRemoteNode(
                    universalContext.nodeId(), notification.getSenderNodeName());
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
        });

        switch (notification.getSubject()) {
            case SUBSCRIBE:
                if (!requestContext.isPrincipal(requestContext.getOptions().getPrincipal("ask.subscribe.allowed"))) {
                    throw new AuthenticationException();
                }

                jobs.run(
                        AskedJob.class,
                        new AskedJob.Parameters(
                                notification.getSubject(),
                                notification.getSenderNodeName(),
                                notification.getSenderFullName(),
                                notification.getSenderGender(),
                                notification.getSenderAvatar(),
                                notification.getMessage()),
                        universalContext.nodeId());
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

                FriendGroup friendGroup = tx.executeRead(
                    () -> friendGroupRepository.findByNodeIdAndId(universalContext.nodeId(), friendGroupId)
                ).orElseThrow(() -> new ValidationFailure("friend-group.not-found"));
                if (!friendGroup.getViewPrincipal().isPublic()) {
                    throw new ValidationFailure("friend-group.not-found");
                }

                jobs.run(
                        AskedJob.class,
                        new AskedJob.Parameters(
                                notification.getSubject(),
                                notification.getSenderNodeName(),
                                notification.getSenderFullName(),
                                notification.getSenderGender(),
                                notification.getSenderAvatar(),
                                friendGroup.getId(),
                                friendGroup.getTitle(),
                                notification.getMessage()),
                        universalContext.nodeId());
                break;
            }
        }
    }

}
