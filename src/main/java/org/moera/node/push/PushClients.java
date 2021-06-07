package org.moera.node.push;

import java.time.Instant;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class PushClients {

    private static Logger log = LoggerFactory.getLogger(PushClients.class);

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
    @Qualifier("pushTaskExecutor")
    private TaskExecutor taskExecutor;

    @Inject
    private PlatformTransactionManager txManager;

    public PushClients(UUID nodeId) {
        this.nodeId = nodeId;
    }

    public void init() {
        clients = pushClientRepository.findAllByNodeId(nodeId).stream()
                .collect(Collectors.toMap(PushClient::getClientId, Function.identity()));
    }

    public void register(PushClient client, SseEmitter emitter) {
        log.info("Registering emitter for node {}, client {}", nodeId, client.getClientId());

        Pusher pusher;
        synchronized (mapLock) {
            clients.putIfAbsent(client.getClientId(), client);
            pusher = pushers.get(client.getClientId());
            if (pusher == null) {
                pusher = new Pusher(this, client.getClientId(), emitter);
                taskAutowire.autowireWithoutRequest(pusher, nodeId);
                pushers.put(client.getClientId(), pusher);
            } else {
                pusher.replaceEmitter(emitter);
            }
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

    private PushPacket buildPacket(String content) {
        long moment = Instant.now().getEpochSecond() * 1000;
        synchronized (lastMomentLock) {
            if (lastMoment < moment) {
                lastMoment = moment;
            } else {
                lastMoment++;
                moment = lastMoment;
            }
        }

        log.info("Assigned moment {}", moment);

        return new PushPacket(moment, content);
    }

    private void storePacket(PushPacket packet) {
        Transaction.executeQuietly(txManager, () -> {
            for (PushClient client : clients.values()) {
                PushNotification pn = new PushNotification();
                pn.setId(UUID.randomUUID());
                pn.setPushClient(client);
                pn.setMoment(packet.getMoment());
                pn.setContent(packet.getContent());
                pushNotificationRepository.save(pn);
            }
            return null;
        });
    }

    public void send(String content) {
        log.info("Sending packet for node {}", nodeId);

        PushPacket packet = buildPacket(content);
        storePacket(packet);

        List<Pusher> clients;
        synchronized (mapLock) {
            clients = new ArrayList<>(pushers.values());
        }
        for (Pusher client : clients) {
            log.info("Sending to client {}", client.getClientId());
            if (!client.getQueue().offer(packet)) {
                unregister(client.getClientId());
                continue;
            }
            if (client.isStopped()) {
                client.setStopped(false);
                taskExecutor.execute(client);
            }
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
