package org.moera.node.notification.send;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.RejectedExecutionException;
import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.commons.util.LogUtil;
import org.moera.node.auth.Scope;
import org.moera.node.config.Config;
import org.moera.node.data.ConnectivityStatus;
import org.moera.node.data.Friend;
import org.moera.node.data.FriendRepository;
import org.moera.node.data.PendingNotification;
import org.moera.node.data.PendingNotificationRepository;
import org.moera.node.data.Subscriber;
import org.moera.node.data.SubscriberRepository;
import org.moera.node.data.SubscriptionType;
import org.moera.node.domain.Domains;
import org.moera.node.domain.DomainsConfiguredEvent;
import org.moera.node.friends.FriendCache;
import org.moera.node.friends.SubscribedCache;
import org.moera.node.global.RequestCounter;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.LiberinManager;
import org.moera.node.liberin.model.SubscriberDeletedLiberin;
import org.moera.node.model.notification.Notification;
import org.moera.node.model.notification.SubscriberNotification;
import org.moera.node.operations.ContactOperations;
import org.moera.node.operations.GrantCache;
import org.moera.node.operations.RemoteConnectivityOperations;
import org.moera.node.task.TaskAutowire;
import org.moera.node.util.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

@Service
public class NotificationSenderPool {

    private static final Duration EXECUTION_REJECTED_DELAY = Duration.of(5, ChronoUnit.MINUTES);
    private static final Duration FROZEN_DELAY = Duration.of(10, ChronoUnit.MINUTES);
    private static final float FROZEN_DELIVERY_LOAD_THRESHOLD = 0.25f;

    private static final Logger log = LoggerFactory.getLogger(NotificationSenderPool.class);

    private final ConcurrentMap<SingleDirection, NotificationSender> senders = new ConcurrentHashMap<>();
    private final List<NotificationSender> pausedSenders = Collections.synchronizedList(new ArrayList<>());

    @Inject
    private Config config;

    @Inject
    private RequestCounter requestCounter;

    @Inject
    private UniversalContext universalContext;

    @Inject
    private Domains domains;

    @Inject
    @Qualifier("notificationSenderTaskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;

    @Inject
    private TaskAutowire taskAutowire;

    @Inject
    private SubscriberRepository subscriberRepository;

    @Inject
    private FriendRepository friendRepository;

    @Inject
    private PendingNotificationRepository pendingNotificationRepository;

    @Inject
    private FriendCache friendCache;

    @Inject
    private SubscribedCache subscribedCache;

    @Inject
    private GrantCache grantCache;

    @Inject
    private ContactOperations contactOperations;

    @Inject
    private RemoteConnectivityOperations remoteConnectivityOperations;

    @Inject
    @Lazy
    private LiberinManager liberinManager;

    @Inject
    private ObjectMapper objectMapper;

    @Inject
    private Transaction tx;

    @EventListener(DomainsConfiguredEvent.class)
    public void init() {
        pendingNotificationRepository.findAllInOrder().forEach(this::resend);
    }

    private void resend(PendingNotification pending) {
        universalContext.associate(pending.getNodeId());
        SingleDirection direction = new SingleDirection(pending.getNodeId(), pending.getNodeName());
        Notification notification;
        try {
            notification = objectMapper.readValue(pending.getNotification(),
                    pending.getNotificationType().getStructure());
        } catch (IOException e) {
            log.error("Error deserializing pending notification {}: {}", pending.getId(), e.getMessage());
            return;
        }
        notification.setType(pending.getNotificationType());
        notification.setPendingNotificationId(pending.getId());
        notification.setCreatedAt(pending.getCreatedAt());
        if (notification instanceof SubscriberNotification) {
            ((SubscriberNotification) notification).setSubscriptionCreatedAt(pending.getSubscriptionCreatedAt());
        }
        send(direction, notification);
    }

    public void send(Direction direction, Notification notification) {
        log.info("Sending notification {}", notification.toLogMessage());
        if (direction instanceof SingleDirection sd) {
            log.info("Sending to node '{}' only, if {}", sd.getNodeName(), direction.getPrincipalFilter());
            sendSingle(sd, notification);
            return;
        }
        if (direction instanceof SubscribersDirection sd) {
            log.info("Sending to '{}' subscribers (feedName = {}, postingId = {}), if {}",
                    sd.getSubscriptionType().getValue(),
                    LogUtil.format(sd.getFeedName()),
                    LogUtil.format(sd.getPostingId()),
                    direction.getPrincipalFilter());

            List<Subscriber> subscribers = switch (sd.getSubscriptionType()) {
                case FEED, USER_LIST -> subscriberRepository.findAllByFeedName(
                        sd.getNodeId(), sd.getSubscriptionType(), sd.getFeedName());
                case POSTING, POSTING_COMMENTS -> subscriberRepository.findAllByEntryId(
                        sd.getNodeId(), sd.getSubscriptionType(), sd.getPostingId());
                case PROFILE -> subscriberRepository.findAllByType(
                        sd.getNodeId(), sd.getSubscriptionType());
            };
            for (Subscriber subscriber : subscribers) {
                SingleDirection dir = new SingleDirection(subscriber.getNodeId(), subscriber.getRemoteNodeName(),
                        direction.getPrincipalFilter());
                Notification nt = notification.clone();
                if (nt instanceof SubscriberNotification) {
                    ((SubscriberNotification) nt).setSubscriberId(subscriber.getId().toString());
                    ((SubscriberNotification) nt).setSubscriptionCreatedAt(subscriber.getCreatedAt());
                }
                sendSingle(dir, nt);
            }
            return;
        }
        if (direction instanceof FriendGroupDirection fd) {
            log.info("Sending to members of '{}' friend group, if {}",
                    LogUtil.format(fd.getFriendGroupId()),
                    direction.getPrincipalFilter());

            List<Friend> friends = friendRepository.findAllByNodeIdAndGroup(fd.getNodeId(), fd.getFriendGroupId());
            for (Friend friend : friends) {
                SingleDirection dir = new SingleDirection(fd.getNodeId(), friend.getRemoteNodeName(),
                        direction.getPrincipalFilter());
                sendSingle(dir, notification.clone());
            }
            return;
        }
        throw new IllegalArgumentException("Unknown direction type");
    }

    private void sendSingle(SingleDirection direction, Notification notification) {
        String nodeName = domains.getDomainOptions(direction.getNodeId()).nodeName();
        long adminScope = grantCache.get(direction.getNodeId(), nodeName);
        boolean adminViewContent = Scope.VIEW_CONTENT.included(adminScope);
        if (!direction.isPermitted(
                Objects.equals(nodeName, direction.getNodeName()) || adminViewContent,
                direction.getNodeName(),
                subscribedCache.isSubscribed(direction.getNodeId(), direction.getNodeName()),
                friendCache.getClientGroupIds(direction.getNodeName()))
        ) {
            return;
        }

        while (true) {
            NotificationSender sender;
            do {
                sender = senders.computeIfAbsent(direction, d -> createSender(d.getNodeId(), d.getNodeName()));
            } while (sender.isStopped());
            try {
                storePending(sender, notification);
                sender.put(notification);
            } catch (InterruptedException e) {
                continue;
            } catch (JsonProcessingException e) {
                log.error("Error serializing notification", e);
            }
            break;
        }
    }

    private NotificationSender createSender(UUID nodeId, String nodeName) {
        log.debug("Creating sender from node ID = {} to '{}'", nodeId, nodeName);
        NotificationSender sender = new NotificationSender(this, nodeName);
        if (nodeId == null) {
            taskAutowire.autowire(sender);
        } else {
            taskAutowire.autowireWithoutRequest(sender, nodeId);
        }

        if (isFrozenNode(sender.getReceiverNodeName())) {
            sender.setPausedTill(Instant.now().plus(FROZEN_DELAY));
            pauseSender(sender);
            return sender;
        }

        try {
            taskExecutor.execute(sender);
        } catch (RejectedExecutionException e) {
            log.warn("Sender was rejected by task executor, pausing");
            sender.setPausedTill(Instant.now().plus(EXECUTION_REJECTED_DELAY));
            pauseSender(sender);
        }
        return sender;
    }

    void deleteSender(UUID nodeId, String nodeName) {
        senders.remove(new SingleDirection(nodeId, nodeName));
    }

    private boolean isFrozenNode(String nodeName) {
        var connectivityStatus = remoteConnectivityOperations.getStatus(nodeName);
        float load = (float) taskExecutor.getActiveCount() / config.getPools().getNotificationSender();
        return connectivityStatus == ConnectivityStatus.FROZEN && load > FROZEN_DELIVERY_LOAD_THRESHOLD;
    }

    void unsubscribe(UUID subscriberId) {
        Subscriber subscriber = tx.executeWriteQuietly(() -> {
            Subscriber subscr = subscriberRepository.findById(subscriberId).orElse(null);
            if (subscr != null) {
                subscriberRepository.delete(subscr);
                if (subscr.getSubscriptionType() == SubscriptionType.FEED) {
                    contactOperations.updateFeedSubscriberCount(subscr.getRemoteNodeName(), -1);
                }
            }
            return subscr;
        });
        if (subscriber != null) {
            liberinManager.send(new SubscriberDeletedLiberin(subscriber).withNodeId(subscriber.getNodeId()));
        }
    }

    private void storePending(NotificationSender sender, Notification notification) throws JsonProcessingException {
        if (notification.getPendingNotificationId() != null) {
            return;
        }
        PendingNotification pending = new PendingNotification();
        pending.setId(UUID.randomUUID());
        pending.setNodeId(sender.getNodeId());
        pending.setNodeName(sender.getReceiverNodeName());
        pending.setNotificationType(notification.getType());
        pending.setNotification(objectMapper.writeValueAsString(notification));
        if (notification instanceof SubscriberNotification) {
            pending.setSubscriptionCreatedAt(((SubscriberNotification) notification).getSubscriptionCreatedAt());
        }
        tx.executeWriteQuietly(() -> {
            pendingNotificationRepository.save(pending);
            notification.setPendingNotificationId(pending.getId());
        });
    }

    void pauseSender(NotificationSender sender) {
        pausedSenders.add(sender);
    }

    void resumeSender(NotificationSender sender) {
        pausedSenders.remove(sender);
    }

    @Scheduled(fixedDelayString = "PT1M")
    public void resumeSenders() {
        try (var ignored = requestCounter.allot()) {
            log.debug("Resuming paused notification senders");

            List<NotificationSender> resumed = pausedSenders.stream()
                    .filter(sender -> Instant.now().compareTo(sender.getPausedTill()) >= 0)
                    .filter(sender -> !isFrozenNode(sender.getReceiverNodeName()))
                    .toList();
            resumed.forEach(sender -> {
                resumeSender(sender);
                taskExecutor.execute(sender);
            });
        }
    }

}
