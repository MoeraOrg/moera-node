package org.moera.node.push;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class PushService {

    private final Map<UUID, PushClients> nodeClients = new ConcurrentHashMap<>();

    public BlockingQueue<PushPacket> acquireQueue(UUID nodeId, String clientId) {
        PushClients clients = nodeClients.computeIfAbsent(nodeId, PushClients::new);
        return clients.acquireQueue(clientId);
    }

    public void releaseQueue(UUID nodeId, String clientId) {
        PushClients clients = nodeClients.get(nodeId);
        if (clients != null) {
            clients.releaseQueue(clientId);
        }
    }

    public void send(UUID nodeId, PushPacket packet) {
        PushClients clients = nodeClients.get(nodeId);
        if (clients != null) {
            clients.send(packet);
        }
    }

}
