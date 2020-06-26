package org.moera.node.notification.send;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.inject.Inject;

import org.moera.node.data.Subscriber;
import org.moera.node.data.SubscriberRepository;
import org.moera.node.data.SubscriptionType;
import org.moera.node.event.EventManager;
import org.moera.node.model.event.SubscriberDeletedEvent;
import org.moera.node.model.notification.Notification;
import org.moera.node.model.notification.SubscriberNotification;
import org.moera.node.task.TaskAutowire;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

@Service
public class NotificationSenderPool {

    private ConcurrentMap<SingleDirection, NotificationSender> senders = new ConcurrentHashMap<>();

    @Inject
    @Qualifier("notificationSenderTaskExecutor")
    private TaskExecutor taskExecutor;

    @Inject
    private TaskAutowire taskAutowire;

    @Inject
    private SubscriberRepository subscriberRepository;

    @Inject
    private EventManager eventManager;

    public void send(Direction direction, Notification notification) {
        if (direction instanceof SingleDirection) {
            sendSingle((SingleDirection) direction, notification);
            return;
        }
        if (direction instanceof SubscribersDirection) {
            SubscribersDirection sd = (SubscribersDirection) direction;
            List<Subscriber> subscribers = Collections.emptyList();
            switch (sd.getSubscriptionType()) {
                case FEED:
                    subscribers = subscriberRepository.findAllByFeedName(
                            sd.getNodeId(), SubscriptionType.FEED, sd.getFeedName());
                    break;
                case POSTING:
                    subscribers = subscriberRepository.findAllByEntryId(
                            sd.getNodeId(), SubscriptionType.POSTING, sd.getPostingId());
                    break;
            }
            for (Subscriber subscriber : subscribers) {
                SingleDirection dir = new SingleDirection(subscriber.getNodeId(), subscriber.getRemoteNodeName());
                Notification nt = notification.clone();
                if (nt instanceof SubscriberNotification) {
                    ((SubscriberNotification) nt).setSubscriberId(subscriber.getId().toString());
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
                sender = senders.computeIfAbsent(direction, d -> createSender(d.getNodeName()));
            } while (sender.isStopped());
            try {
                sender.put(notification);
            } catch (InterruptedException e) {
                continue;
            }
            break;
        }
    }

    private NotificationSender createSender(String nodeName) {
        NotificationSender sender = new NotificationSender(this, nodeName);
        taskAutowire.autowire(sender);
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

}
