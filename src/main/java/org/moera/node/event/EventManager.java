package org.moera.node.event;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.moera.lib.node.types.Scope;
import org.moera.lib.util.LogUtil;
import org.moera.node.auth.AuthSecrets;
import org.moera.node.auth.AuthenticationManager;
import org.moera.node.auth.CarteAuthInfo;
import org.moera.node.auth.InvalidTokenException;
import org.moera.node.data.Token;
import org.moera.node.domain.Domains;
import org.moera.node.friends.FriendCache;
import org.moera.node.friends.SubscribedCache;
import org.moera.node.global.RequestCounter;
import org.moera.node.global.UniversalContext;
import org.moera.node.model.event.Event;
import org.moera.node.model.event.PingEvent;
import org.moera.node.operations.GrantCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

@Service
public class EventManager {

    private static final Logger log = LoggerFactory.getLogger(EventManager.class);

    private static final String USER_PREFIX = "/user";
    private static final String EVENT_DESTINATION = "/queue";
    private static final String TOKEN_HEADER = "token";

    @Inject
    private RequestCounter requestCounter;

    @Inject
    private UniversalContext universalContext;

    @Inject
    private Domains domains;

    @Inject
    @Lazy
    private SimpMessagingTemplate messagingTemplate;

    @Inject
    private AuthenticationManager authenticationManager;

    @Inject
    private FriendCache friendCache;

    @Inject
    private SubscribedCache subscribedCache;

    @Inject
    private GrantCache grantCache;

    private final Map<String, EventSubscriber> subscribers = new ConcurrentHashMap<>();
    private final List<EventPacket> queue = new ArrayList<>();
    private final long startedAt = Instant.now().getEpochSecond();
    private int lastOrdinal = 0;
    private final ReadWriteLock eventsLock = new ReentrantReadWriteLock();
    private final Object deliverySignal = new Object();

    @EventListener(SessionConnectEvent.class)
    public void sessionConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        if (accessor.getHost() == null) {
            log.debug("Ignoring session {} without a host", accessor.getSessionId());
            return;
        }
        UUID nodeId = domains.getDomainNodeId(accessor.getHost());
        nodeId = nodeId != null ? nodeId : domains.getDomainNodeId(Domains.DEFAULT_DOMAIN);
        universalContext.associate(nodeId);

        AuthSecrets secrets = new AuthSecrets(accessor.getFirstNativeHeader(TOKEN_HEADER));
        long adminScope = 0;
        long clientScope = 0;
        String clientName = null;
        boolean owner = false;
        String[] friendGroups = null;
        boolean subscribedToClient = false;
        try {
            Token token = authenticationManager.getToken(secrets.token, nodeId);
            if (token != null) {
                adminScope = token.getAuthScope() != 0 ? token.getAuthScope() : Scope.ALL.getMask();
                owner = true;
            } else if (secrets.carte != null) {
                CarteAuthInfo carteAuthInfo = authenticationManager.getCarte(secrets.carte, getRemoteAddress(accessor));
                if (carteAuthInfo != null) {
                    clientName = carteAuthInfo.getClientName();
                    clientScope = carteAuthInfo.getClientScope();
                    friendGroups = friendCache.getClientGroupIds(clientName);
                    subscribedToClient = subscribedCache.isSubscribed(clientName);
                    owner = Objects.equals(clientName, universalContext.nodeName());
                    adminScope = carteAuthInfo.getAdminScope();
                    adminScope &= grantCache.get(universalContext.nodeId(), clientName);
                    if (owner) {
                        adminScope |= clientScope & Scope.VIEW_ALL.getMask();
                    }
                }
            }
        } catch (InvalidTokenException | UnknownHostException e) {
            // Ignore, the client will detect the problem from REST API requests
        }
        log.info("Session connect, id = {} clientName = {}", accessor.getSessionId(), LogUtil.format(clientName));

        EventSubscriber subscriber = new EventSubscriber();
        subscriber.setNodeId(nodeId);
        subscriber.setOptions(domains.getDomainOptions(nodeId));
        subscriber.setSessionId(accessor.getSessionId());
        subscriber.setAdminScope(adminScope);
        subscriber.setClientScope(clientScope);
        subscriber.setClientName(clientName);
        subscriber.setOwner(owner);
        subscriber.setSubscribedToClient(subscribedToClient);
        subscriber.setFriendGroups(friendGroups);
        subscribers.put(accessor.getSessionId(), subscriber);
    }

    private InetAddress getRemoteAddress(StompHeaderAccessor accessor) throws UnknownHostException {
        String address = "127.0.0.1";
        if (accessor.getSessionAttributes() != null) {
            Object ip = accessor.getSessionAttributes().get("ip");
            if (ip != null) {
                address = ip.toString();
            }
            if (address.equals("/127.0.0.1")) { // Don't understand why '/' is here
                address = "127.0.0.1";
            }
        }
        return InetAddress.getByName(address);
    }

    @EventListener(SessionSubscribeEvent.class)
    public void subscribed(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        if (!(USER_PREFIX + EVENT_DESTINATION).equals(accessor.getDestination())) {
            return;
        }

        SeenHeader.Details seen = SeenHeader.parse(accessor);
        initLoggingDomain(accessor.getSessionId());
        log.info("Session subscribed, id = {} seen = {}/{}",
                accessor.getSessionId(),
                LogUtil.format(seen.queueStartedAt),
                LogUtil.format(seen.lastEvent));

        EventSubscriber subscriber = subscribers.get(accessor.getSessionId());
        if (subscriber == null) {
            return;
        }
        if (seen.queueStartedAt == null) {
            subscriber.setLastEventSeen(lastOrdinal);
        } else if (seen.queueStartedAt != startedAt) {
            subscriber.setLastEventSeen(0);
        } else {
            subscriber.setLastEventSeen(seen.lastEvent);
        }
        subscriber.setSubscribed(true);
    }

    @EventListener(SessionUnsubscribeEvent.class)
    public void unsubscribed(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        if (!(USER_PREFIX + EVENT_DESTINATION).equals(accessor.getDestination())) {
            return;
        }
        initLoggingDomain(accessor.getSessionId());
        subscribers.remove(accessor.getSessionId());
        log.info("Session unsubscribed, id = {}", accessor.getSessionId());
    }

    @EventListener(SessionDisconnectEvent.class)
    public void disconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        initLoggingDomain(accessor.getSessionId());
        subscribers.remove(accessor.getSessionId());
        log.info("Session disconnected, id = {}", accessor.getSessionId());
    }

    private void initLoggingDomain(String sessionId) {
        EventSubscriber subscriber = subscribers.get(sessionId);
        if (subscriber != null) {
            MDC.put("domain", domains.getDomainName(subscriber.getNodeId()));
        }
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
                    // ignore
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

    public void send(UUID nodeId, Event event) {
        send(nodeId, null, event);
    }

    public void send(UUID nodeId, String clientId, Event event) {
        MDC.put("domain", domains.getDomainName(nodeId));
        log.info("Event arrived: {} for {}", event.toLogMessage(), event.getFilter());

        eventsLock.writeLock().lock();
        try {
            purge();
            EventPacket packet = new EventPacket();
            packet.setNodeId(nodeId);
            packet.setQueueStartedAt(startedAt);
            packet.setOrdinal(++lastOrdinal);
            packet.setSentAt(Instant.now().getEpochSecond());
            packet.setCid(clientId);
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
    public void everyMinute() {
        try (var ignored = requestCounter.allot()) {
            log.debug("Retrying event delivery");

            retryDelivery();
            pingAll();
        }
    }

    private void retryDelivery() {
        synchronized (deliverySignal) {
            deliverySignal.notifyAll();
        }
    }

    private void pingAll() {
        Set<UUID> nodeIds = subscribers.values().stream()
                .filter(EventSubscriber::isSubscribed)
                .map(EventSubscriber::getNodeId)
                .collect(Collectors.toSet());
        nodeIds.forEach(nodeId -> send(nodeId, new PingEvent()));
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
                    .filter(EventSubscriber::isSubscribed)
                    .filter(sub -> sub.getLastEventSeen() < last)
                    .forEach(this::deliver);
        } finally {
            eventsLock.readLock().unlock();
        }
    }

    private void deliver(EventSubscriber subscriber) {
        initLoggingDomain(subscriber.getSessionId());
        log.debug("Delivering events to {}", subscriber.getSessionId());

        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor
                .create(SimpMessageType.MESSAGE);
        headerAccessor.setSessionId(subscriber.getSessionId());
        MessageHeaders headers = headerAccessor.getMessageHeaders();

        int first = queue.get(0).getOrdinal();
        int beginIndex = Math.max(0, subscriber.getLastEventSeen() - first + 1);
        try {
            for (int i = beginIndex; i < queue.size(); i++) {
                EventPacket packet = queue.get(i);
                if (!Objects.equals(packet.getNodeId(), subscriber.getNodeId())
                        || !packet.getEvent().isPermitted(subscriber)) {
                    subscriber.setLastEventSeen(first + i);
                    continue;
                }
                log.debug("Sending event {}: {}", first + i, packet.getEvent().getType());
                packet.getEvent().protect(subscriber);
                messagingTemplate.convertAndSendToUser(subscriber.getSessionId(), EVENT_DESTINATION, packet, headers);
                subscriber.setLastEventSeen(first + i);
            }
        } catch (MessagingException e) {
            log.error("Error sending event", e);
        }
    }

}
