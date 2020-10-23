package org.moera.node.notification.send;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.commons.util.LogUtil;
import org.moera.node.data.PendingNotification;
import org.moera.node.data.PendingNotificationRepository;
import org.moera.node.data.Subscriber;
import org.moera.node.data.SubscriberRepository;
import org.moera.node.domain.DomainsConfiguredEvent;
import org.moera.node.event.EventManager;
import org.moera.node.model.event.SubscriberDeletedEvent;
import org.moera.node.model.notification.Notification;
import org.moera.node.model.notification.SubscriberNotification;
import org.moera.node.task.TaskAutowire;
import org.moera.node.util.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

@Service
public class NotificationSenderPool {

    private static Logger log = LoggerFactory.getLogger(NotificationSenderPool.class);

    private ConcurrentMap<SingleDirection, NotificationSender> senders = new ConcurrentHashMap<>();

    @Inject
    @Qualifier("notificationSenderTaskExecutor")
    private TaskExecutor taskExecutor;

    @Inject
    private TaskAutowire taskAutowire;

    @Inject
    private SubscriberRepository subscriberRepository;

    @Inject
    private PendingNotificationRepository pendingNotificationRepository;

    @Inject
    private EventManager eventManager;

    @Inject
    private ObjectMapper objectMapper;

    @Inject
    private PlatformTransactionManager txManager;

    @EventListener(DomainsConfiguredEvent.class)
    public void init() {
        pendingNotificationRepository.findAllInOrder().forEach(this::resend);
    }

    private void resend(PendingNotification pending) {
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
        log.info("Sending notification of type '{}'", notification.getType().getValue());
        if (direction instanceof SingleDirection) {
            log.info("Sending to node '{}' only", ((SingleDirection) direction).getNodeName());
            sendSingle((SingleDirection) direction, notification);
            return;
        }
        if (direction instanceof SubscribersDirection) {
            SubscribersDirection sd = (SubscribersDirection) direction;

            log.info("Sending to '{}' subscribers (feedName = {}, postingId = {})",
                    sd.getSubscriptionType().getValue(),
                    LogUtil.format(sd.getFeedName()),
                    LogUtil.format(sd.getPostingId()));

            List<Subscriber> subscribers = Collections.emptyList();
            switch (sd.getSubscriptionType()) {
                case FEED:
                    subscribers = subscriberRepository.findAllByFeedName(
                            sd.getNodeId(), sd.getSubscriptionType(), sd.getFeedName());
                    break;
                case POSTING:
                case POSTING_COMMENTS:
                    subscribers = subscriberRepository.findAllByEntryId(
                            sd.getNodeId(), sd.getSubscriptionType(), sd.getPostingId());
                    break;
            }
            for (Subscriber subscriber : subscribers) {
                SingleDirection dir = new SingleDirection(subscriber.getNodeId(), subscriber.getRemoteNodeName());
                Notification nt = notification.clone();
                if (nt instanceof SubscriberNotification) {
                    ((SubscriberNotification) nt).setSubscriberId(subscriber.getId().toString());
                    ((SubscriberNotification) nt).setSubscriptionCreatedAt(subscriber.getCreatedAt());
                }
                sendSingle(dir, nt);
            }
            return;
        }
        throw new IllegalArgumentException("Unknown direction type");
    }

    private void sendSingle(SingleDirection direction, Notification notification) {
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
        log.info("Creating sender from node ID = {} to '{}'", nodeId, nodeName);
        NotificationSender sender = new NotificationSender(this, nodeName);
        if (nodeId == null) {
            taskAutowire.autowire(sender);
        } else {
            taskAutowire.autowireWithoutRequest(sender, nodeId);
        }
        taskExecutor.execute(sender);
        return sender;
    }

    void deleteSender(UUID nodeId, String nodeName) {
        senders.remove(new SingleDirection(nodeId, nodeName));
    }

    void unsubscribe(UUID subscriberId) {
        subscriberRepository.findById(subscriberId).ifPresent(subscriber -> {
            eventManager.send(subscriber.getNodeId(), new SubscriberDeletedEvent(subscriber));
            subscriberRepository.delete(subscriber);
        });
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
        Transaction.executeQuietly(txManager, () -> {
            pendingNotificationRepository.save(pending);
            notification.setPendingNotificationId(pending.getId());
            return null;
        });
    }

}
