package org.moera.node.push;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;

import org.moera.node.data.PushClient;
import org.moera.node.data.PushClientRepository;
import org.moera.node.data.PushNotification;
import org.moera.node.data.PushNotificationRepository;
import org.moera.node.task.TaskAutowire;
import org.moera.node.util.Transaction;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class PushClients {

    private static final Logger log = LoggerFactory.getLogger(PushClients.class);

    private final UUID nodeId;
    private Map<String, PushClient> clients;
    private final Map<String, Pusher> pushers = new HashMap<>();
    private final Object mapLock = new Object();
    private long lastMoment;
    private final Object lastMomentLock = new Object();

    @Inject
    private PushClientRepository pushClientRepository;

    @Inject
    private PushNotificationRepository pushNotificationRepository;

    @Inject
    private TaskAutowire taskAutowire;

    @Inject
    private Transaction tx;

    public PushClients(UUID nodeId) {
        this.nodeId = nodeId;
    }

    public void init() {
        clients = pushClientRepository.findAllByNodeId(nodeId).stream()
                .collect(Collectors.toMap(PushClient::getClientId, Function.identity()));
    }

    public void register(PushClient client, SseEmitter emitter, long lastSeenMoment) {
        log.info("Registering emitter for node {}, client {}", nodeId, client.getClientId());

        Pusher pusher;
        synchronized (mapLock) {
            clients.putIfAbsent(client.getClientId(), client);
            pusher = pushers.get(client.getClientId());
            if (pusher != null) {
                pusher.complete();
            }
            pusher = new Pusher(this, client, emitter);
            taskAutowire.autowireWithoutRequest(pusher, nodeId);
            pushers.put(client.getClientId(), pusher);
            pusher.setLastSentMoment(lastSeenMoment);
            pusher.activate();
        }
    }

    void unregister(String clientId) {
        log.info("Unregistering emitter for node {}, client {}", nodeId, clientId);

        synchronized (mapLock) {
            Pusher pusher = pushers.get(clientId);
            if (pusher != null) {
                pushers.remove(clientId);
            }
        }
    }

    public void delete(String clientId) {
        log.info("Deleting emitter for node {}, client {}", nodeId, clientId);

        synchronized (mapLock) {
            clients.remove(clientId);
            Pusher pusher = pushers.get(clientId);
            if (pusher != null) {
                pusher.complete();
                pushers.remove(clientId);
            }
        }
    }

    private PushPacket buildPacket(String content) {
        long moment = Util.currentMoment();
        synchronized (lastMomentLock) {
            if (lastMoment < moment) {
                lastMoment = moment;
            } else {
                lastMoment++;
                moment = lastMoment;
            }
        }

        log.debug("Assigned moment {}", moment);

        return new PushPacket(moment, content);
    }

    private void storePacket(PushPacket packet) {
        tx.executeWriteQuietly(
            () -> {
                for (PushClient client : clients.values()) {
                    PushNotification pn = new PushNotification();
                    pn.setId(UUID.randomUUID());
                    pn.setPushClient(client);
                    pn.setMoment(packet.getMoment());
                    pn.setContent(packet.getContent());
                    pushNotificationRepository.save(pn);
                }
            },
            e -> log.error("Error storing a push packet", e)
        );
    }

    public void send(String content) {
        log.debug("Sending packet for node {}", nodeId);

        PushPacket packet = buildPacket(content);
        storePacket(packet);

        List<Pusher> pusherList;
        synchronized (mapLock) {
            pusherList = new ArrayList<>(pushers.values());
        }
        for (Pusher pusher : pusherList) {
            log.debug("Sending to client {}", pusher.getClientId());
            pusher.offer(packet);
        }
    }

    void updateLastSeenAt() {
        pushers.values().stream()
                .map(Pusher::getClientId)
                .map(clients::get)
                .map(PushClient::getId)
                .forEach(id -> pushClientRepository.updateLastSeenAt(id, Util.now()));
    }

}
