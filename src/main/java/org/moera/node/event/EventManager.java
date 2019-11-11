package org.moera.node.event;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.moera.commons.util.LogUtil;
import org.moera.node.event.model.Event;
import org.moera.node.global.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

@Service
public class EventManager {

    private static Logger log = LoggerFactory.getLogger(EventManager.class);

    private static final String USER_PREFIX = "/user";
    private static final String EVENT_DESTINATION = "/queue";

    @Inject
    private RequestContext requestContext;

    @Inject
    private SimpMessagingTemplate messagingTemplate;

    private Map<String, EventSubscriber> subscribers = new ConcurrentHashMap<>();
    private List<EventPacket> queue = new ArrayList<>();
    private final long startedAt = Instant.now().getEpochSecond();
    private int lastOrdinal = 0;
    private ReadWriteLock eventsLock = new ReentrantReadWriteLock();
    private final Object deliverySignal = new Object();

    @EventListener(SessionConnectedEvent.class)
    public void sessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        log.info("Session connected, id = {}", accessor.getSessionId());
    }

    @EventListener(SessionSubscribeEvent.class)
    public void subscribed(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        if (!(USER_PREFIX + EVENT_DESTINATION).equals(accessor.getDestination())) {
            return;
        }

        SeenHeader.Details seen = SeenHeader.parse(accessor);
        log.info("Session subscribed, id = {} seen = {}/{}",
                accessor.getSessionId(),
                LogUtil.format(seen.queueStartedAt),
                LogUtil.format(seen.lastEvent));

        EventSubscriber subscriber = new EventSubscriber();
        subscriber.setSessionId(accessor.getSessionId());
        if (seen.queueStartedAt == null) {
            subscriber.setLastEventSeen(lastOrdinal);
        } else if (seen.queueStartedAt != startedAt) {
            subscriber.setLastEventSeen(0);
        } else {
            subscriber.setLastEventSeen(seen.lastEvent);
        }
        subscribers.put(accessor.getSessionId(), subscriber);
    }

    @EventListener(SessionUnsubscribeEvent.class)
    public void unsubscribed(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        if (!(USER_PREFIX + EVENT_DESTINATION).equals(accessor.getDestination())) {
            return;
        }
        subscribers.remove(accessor.getSessionId());
        log.info("Session unsubscribed, id = {}", accessor.getSessionId());
    }

    @EventListener(SessionDisconnectEvent.class)
    public void unsubscribed(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        subscribers.remove(accessor.getSessionId());
        log.info("Session disconnected, id = {}", accessor.getSessionId());
    }

    @PostConstruct
    public void init() {
        Thread deliveryThread = new Thread(() -> {
            while (true) {
                try {
                    synchronized (deliverySignal) {
                        deliverySignal.wait();
                    }
                } catch (InterruptedException e) {
                }
                deliver();
            }
        });
        deliveryThread.setName("eventDelivery");
        deliveryThread.setDaemon(true);
        deliveryThread.start();
    }

    private void purge() {
        long boundary = Instant.now().minus(10, ChronoUnit.MINUTES).getEpochSecond();
        queue.removeIf(packet -> packet.getSentAt() < boundary);
    }

    public void send(Event event) {
        log.info("Event arrived: {}", event.getType());

        eventsLock.writeLock().lock();
        try {
            purge();
            EventPacket packet = new EventPacket();
            packet.setQueueStartedAt(startedAt);
            packet.setOrdinal(++lastOrdinal);
            packet.setSentAt(Instant.now().getEpochSecond());
            packet.setCid(requestContext.getClientId());
            packet.setEvent(event);
            queue.add(packet);
        } finally {
            eventsLock.writeLock().unlock();
        }
        synchronized (deliverySignal) {
            deliverySignal.notifyAll();
        }
    }

    @Scheduled(fixedDelayString = "PT1M")
    public void retryDelivery() {
        synchronized (deliverySignal) {
            deliverySignal.notifyAll();
        }
    }

    private void deliver() {
        log.debug("Delivery of events");

        eventsLock.readLock().lock();
        try {
            if (queue.isEmpty()) {
                return;
            }
            int last = queue.get(0).getOrdinal() + queue.size() - 1;
            subscribers.values().stream()
                    .filter(sub -> sub.getLastEventSeen() < last)
                    .forEach(this::deliver);
        } finally {
            eventsLock.readLock().unlock();
        }
    }

    private void deliver(EventSubscriber subscriber) {
        log.debug("Delivering events to {}", subscriber.getSessionId());

        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor
                .create(SimpMessageType.MESSAGE);
        headerAccessor.setSessionId(subscriber.getSessionId());
        MessageHeaders headers = headerAccessor.getMessageHeaders();

        int first = queue.get(0).getOrdinal();
        int beginIndex = Math.max(0, subscriber.getLastEventSeen() - first + 1);
        try {
            for (int i = beginIndex; i < queue.size(); i++) {
                log.debug("Sending event {}: {}", first + i, queue.get(i).getEvent().getType());
                messagingTemplate.convertAndSendToUser(subscriber.getSessionId(), EVENT_DESTINATION,
                        queue.get(i), headers);
                subscriber.setLastEventSeen(first + i);
            }
        } catch (MessagingException e) {
            log.error("Error sending event", e);
        }
    }

}
