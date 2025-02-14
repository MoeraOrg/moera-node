package org.moera.node.rest;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import org.moera.lib.node.types.Result;
import org.moera.lib.node.types.Scope;
import org.moera.lib.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.data.PushClient;
import org.moera.node.data.PushClientRepository;
import org.moera.node.domain.Domains;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.global.RequestCounter;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.ValidationFailure;
import org.moera.node.push.PushService;
import org.moera.node.sse.StreamEmitter;
import org.moera.node.util.Transaction;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@ApiController
@RequestMapping("/moera/api/push")
@NoCache
public class PushController {

    private static final Logger log = LoggerFactory.getLogger(PushController.class);

    @Inject
    private RequestCounter requestCounter;

    @Inject
    private RequestContext requestContext;

    @Inject
    private PushService pushService;

    @Inject
    private Domains domains;

    @Inject
    private PushClientRepository pushClientRepository;

    @Inject
    private Transaction tx;

    @Inject
    @PersistenceContext
    private EntityManager entityManager;

    @GetMapping(value = "/{clientId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Admin(Scope.OTHER)
    public StreamEmitter get(@PathVariable String clientId,
                             @RequestParam(name = "after", required = false) Long after,
                             @RequestHeader(value = "Last-Event-ID", required = false) Long lastEventId)
            throws IOException {

        log.info("GET /push/{clientId} (clientId = {}, after = {}, Last-Event-ID = {})",
                LogUtil.format(clientId), LogUtil.format(after), LogUtil.format(lastEventId));

        if (ObjectUtils.isEmpty(clientId)) {
            throw new ValidationFailure("push.clientId.blank");
        }
        if (clientId.length() > 40) {
            throw new ValidationFailure("push.clientId.wrong-size");
        }

        long lastSeenMoment = after != null ? after : (lastEventId != null ? lastEventId : 0);

        PushClient client = getClient(clientId);
        updateLastSeenAt(client);

        StreamEmitter emitter = new StreamEmitter();
        emitter.send(StreamEmitter.event().comment("ברוך הבא")); // To send HTTP headers immediately
        entityManager.detach(client);
        pushService.register(requestContext.nodeId(), client, emitter, lastSeenMoment);
        return emitter;
    }

    private PushClient getClient(String clientId) {
        PushClient client = pushClientRepository.findByClientId(requestContext.nodeId(), clientId).orElse(null);
        if (client != null) {
            return client;
        }

        int count = pushClientRepository.countAllByNodeId(requestContext.nodeId());
        if (count >= requestContext.getOptions().getInt("push.client.max-number")) {
            throw new OperationFailure("push.too-many-clients");
        }

        try {
            client = tx.executeWrite(() -> {
                PushClient clt = new PushClient();
                clt.setId(UUID.randomUUID());
                clt.setNodeId(requestContext.nodeId());
                clt.setClientId(clientId);
                return pushClientRepository.save(clt);
            });
        } catch (DataIntegrityViolationException e) {
            log.info("PushClient is just created in another thread, using it");
            client = pushClientRepository.findByClientId(requestContext.nodeId(), clientId).orElseThrow();
        }
        return client;
    }

    private void updateLastSeenAt(PushClient client) {
        tx.executeWrite(() -> {
            client.setLastSeenAt(Util.now());
            pushClientRepository.save(client);
        });
    }

    @DeleteMapping("/{clientId}")
    @Admin(Scope.OTHER)
    @Transactional
    public Result delete(@PathVariable String clientId) {
        log.info("DELETE /push/{clientId} (clientId = {})", LogUtil.format(clientId));

        PushClient client = pushClientRepository.findByClientId(requestContext.nodeId(), clientId)
                .orElseThrow(() -> new ObjectNotFoundFailure("push.not-found"));
        pushService.delete(requestContext.nodeId(), client.getClientId());
        pushClientRepository.delete(client);

        return Result.OK;
    }

    @Scheduled(fixedDelayString = "P1D")
    @Transactional
    public void purgeInactive() {
        try (var ignored = requestCounter.allot()) {
            log.info("Purging inactive push service clients");

            for (String domainName : domains.getWarmDomainNames()) {
                UUID nodeId = domains.getDomainNodeId(domainName);
                Duration ttl = domains.getDomainOptions(domainName).getDuration("push.client.lifetime").getDuration();
                Timestamp lastSeenAt = Timestamp.from(Instant.now().minus(ttl));
                Collection<PushClient> clients = pushClientRepository.findInactive(nodeId, lastSeenAt);
                for (PushClient client : clients) {
                    pushService.delete(nodeId, client.getClientId());
                    pushClientRepository.delete(client);
                }
            }
        }
    }

}
