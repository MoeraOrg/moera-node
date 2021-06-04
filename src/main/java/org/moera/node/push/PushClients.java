package org.moera.node.push;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;

public class PushClients {

    private static Logger log = LoggerFactory.getLogger(PushClients.class);

    private final UUID nodeId;
    private final Map<String, PushSource> sources = new HashMap<>();
    private final Object mapLock = new Object();

    public PushClients(UUID nodeId) {
        this.nodeId = nodeId;
    }

    public BlockingQueue<PushPacket> acquireQueue(String clientId) {
        log.info("Acquire queue for node {}, client {}", nodeId, clientId);

        PushSource source;
        synchronized (mapLock) {
            source = sources.get(clientId);
            if (source == null) {
                source = new PushSource();
                sources.put(clientId, source);
            }
        }
        if (source.getThread().getId() != Thread.currentThread().getId()) {
            Thread thread = source.getThread();
            source.setThread(Thread.currentThread());
            thread.interrupt();
        }
        return source.getQueue();
    }

    public void releaseQueue(String clientId) {
        log.info("Release queue for node {}, client {}", nodeId, clientId);

        synchronized (mapLock) {
            PushSource source = sources.get(clientId);
            if (source != null && source.getThread().getId() == Thread.currentThread().getId()) {
                sources.remove(clientId);
            }
        }
    }

    private void dropQueue(String clientId) {
        log.info("Drop queue for node {}, client {}", nodeId, clientId);

        PushSource source;
        synchronized (mapLock) {
            source = sources.get(clientId);
            if (source != null) {
                sources.remove(clientId);
            }
        }
        if (source != null) {
            source.getThread().interrupt();
        }
    }

    public void send(PushPacket packet) {
        log.info("Send packet {} for node {}", packet.getId(), nodeId);

        List<Pair<String, BlockingQueue<PushPacket>>> clients;
        synchronized (mapLock) {
            clients = sources.entrySet().stream()
                    .map(e -> Pair.of(e.getKey(), e.getValue().getQueue()))
                    .collect(Collectors.toList());
        }
        for (var client : clients) {
            log.info("Sending to client {}", client.getFirst());
            if (!client.getSecond().offer(packet)) {
                dropQueue(client.getFirst());
            }
        }
    }

}
