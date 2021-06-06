package org.moera.node.push;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class PushService {

    private final Map<UUID, PushClients> nodeClients = new ConcurrentHashMap<>();

    @Inject
    private AutowireCapableBeanFactory autowireCapableBeanFactory;

    public void register(UUID nodeId, String clientId, SseEmitter emitter) {
        PushClients clients = nodeClients.computeIfAbsent(nodeId, this::createClients);
        clients.register(clientId, emitter);
    }

    private PushClients createClients(UUID nodeId) {
        PushClients clients = new PushClients(nodeId);
        autowireCapableBeanFactory.autowireBean(clients);
        return clients;
    }

    public void send(UUID nodeId, String content) {
        PushClients clients = nodeClients.get(nodeId);
        if (clients != null) {
            clients.send(content);
        }
    }

}
