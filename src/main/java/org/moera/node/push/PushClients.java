package org.moera.node.push;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.inject.Inject;

import org.moera.node.task.TaskAutowire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class PushClients {

    private static Logger log = LoggerFactory.getLogger(PushClients.class);

    private final UUID nodeId;
    private final Map<String, Pusher> pushers = new HashMap<>();
    private final Object mapLock = new Object();
    private long lastMoment;
    private final Object lastMomentLock = new Object();

    @Inject
    private TaskAutowire taskAutowire;

    @Inject
    @Qualifier("pushTaskExecutor")
    private TaskExecutor taskExecutor;

    public PushClients(UUID nodeId) {
        this.nodeId = nodeId;
    }

    public void register(String clientId, SseEmitter emitter) {
        log.info("Registering emitter for node {}, client {}", nodeId, clientId);

        Pusher pusher;
        synchronized (mapLock) {
            pusher = pushers.get(clientId);
            if (pusher == null) {
                pusher = new Pusher(this, clientId, emitter);
                taskAutowire.autowireWithoutRequest(pusher, nodeId);
                pushers.put(clientId, pusher);
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

    public void send(String content) {
        log.info("Sending packet for node {}", nodeId);

        PushPacket packet = buildPacket(content);
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

}
