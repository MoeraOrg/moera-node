package org.moera.node.push;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.transaction.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.node.data.PushClient;
import org.moera.node.data.PushClientRepository;
import org.moera.node.data.PushNotificationRepository;
import org.moera.node.domain.Domains;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class PushService {

    private static final Logger log = LoggerFactory.getLogger(PushService.class);

    private final Map<UUID, PushClients> nodeClients = new ConcurrentHashMap<>();

    @Inject
    private AutowireCapableBeanFactory autowireCapableBeanFactory;

    @Inject
    private Domains domains;

    @Inject
    private PushClientRepository pushClientRepository;

    @Inject
    private PushNotificationRepository pushNotificationRepository;

    @Inject
    private ObjectMapper objectMapper;

    public void register(UUID nodeId, PushClient client, SseEmitter emitter, long lastSeenMoment) {
        getClients(nodeId).register(client, emitter, lastSeenMoment);
    }

    public void delete(UUID nodeId, String clientId) {
        getClients(nodeId).delete(clientId);
    }

    public void send(UUID nodeId, String content) {
        getClients(nodeId).send(content);
    }

    public void send(UUID nodeId, PushContent pushContent) {
        String content;
        try {
            content = objectMapper.writeValueAsString(pushContent);
        } catch (JsonProcessingException e) {
            log.error("Error encoding a story for Push notification", e);
            return;
        }
        send(nodeId, content);
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
        try {
            nodeClients.values().forEach(PushClients::updateLastSeenAt);
        } catch (Exception e) {
            log.error("Error updating last seen timestamp of push notifications", e);
        }
    }

    @Scheduled(fixedDelayString = "PT1H")
    @Transactional
    public void purgeUnsent() {
        try {
            for (String domainName : domains.getAllDomainNames()) {
                UUID nodeId = domains.getDomainNodeId(domainName);
                Duration ttl = domains.getDomainOptions(domainName)
                        .getDuration("push.notification.lifetime").getDuration();
                long lastMoment = Instant.now().minus(ttl).getEpochSecond() * 1000;
                pushClientRepository.findAllByNodeId(nodeId)
                        .forEach(client -> pushNotificationRepository.deleteTill(client.getId(), lastMoment));
            }
        } catch (Exception e) {
            log.error("Error purging unsent push notifications", e);
        }
    }

}
