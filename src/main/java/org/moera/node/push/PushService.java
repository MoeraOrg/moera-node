package org.moera.node.push;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.moera.node.data.PushClient;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class PushService {

    private final Map<UUID, PushClients> nodeClients = new ConcurrentHashMap<>();

    @Inject
    private AutowireCapableBeanFactory autowireCapableBeanFactory;

    public void register(UUID nodeId, PushClient client, SseEmitter emitter, long lastSeenMoment) {
        getClients(nodeId).register(client, emitter, lastSeenMoment);
    }

    public void send(UUID nodeId, String content) {
        getClients(nodeId).send(content);
    }

    private PushClients getClients(UUID nodeId) {
        return nodeClients.computeIfAbsent(nodeId, this::createClients);
    }

    private PushClients createClients(UUID nodeId) {
        PushClients clients = new PushClients(nodeId);
        autowireCapableBeanFactory.autowireBean(clients);
        clients.init();
        return clients;
    }

    @Scheduled(fixedDelayString = "PT1M")
    @Transactional
    public void updateLastSeenAt() {
        nodeClients.values().forEach(PushClients::updateLastSeenAt);
    }

}
