package org.moera.node.rest;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.data.PushClient;
import org.moera.node.data.PushClientRepository;
import org.moera.node.domain.Domains;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.Result;
import org.moera.node.model.ValidationFailure;
import org.moera.node.push.PushService;
import org.moera.node.util.Transaction;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ApiController
@RequestMapping("/moera/api/push")
@NoCache
public class PushController {

    private static Logger log = LoggerFactory.getLogger(PushController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private PushService pushService;

    @Inject
    private Domains domains;

    @Inject
    private PushClientRepository pushClientRepository;

    @Inject
    private PlatformTransactionManager txManager;

    @GetMapping("/{clientId}")
    @Admin
    public SseEmitter get(@PathVariable String clientId,
                          @RequestParam(name = "after", required = false) Long after,
                          @RequestHeader(value = "Last-Event-ID", required = false) Long lastEventId)
            throws Throwable {

        long lastSeenMoment = after != null ? after : (lastEventId != null ? lastEventId : 0);

        log.info("GET /push/{clientId} (clientId = {}, lastSeenMoment = {})",
                LogUtil.format(clientId), LogUtil.format(lastSeenMoment));

        if (StringUtils.isEmpty(clientId)) {
            throw new ValidationFailure("push.clientId.blank");
        }

        PushClient client = getClient(clientId);
        updateLastSeenAt(client);

        SseEmitter sseEmitter = new SseEmitter(Long.MAX_VALUE);
        pushService.register(requestContext.nodeId(), client, sseEmitter, lastSeenMoment);
        return sseEmitter;
    }

    private PushClient getClient(String clientId) throws Throwable {
        PushClient client = pushClientRepository.findByClientId(requestContext.nodeId(), clientId).orElse(null);
        if (client != null) {
            return client;
        }

        int count = pushClientRepository.countAllByNodeId(requestContext.nodeId());
        if (count >= requestContext.getOptions().getInt("push.client.max-number")) {
            throw new OperationFailure("push.too-many-clients");
        }

        try {
            client = Transaction.execute(txManager, () -> {
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

    private void updateLastSeenAt(PushClient client) throws Throwable {
        Transaction.execute(txManager, () -> {
            client.setLastSeenAt(Util.now());
            pushClientRepository.save(client);
            return null;
        });
    }

    @DeleteMapping("/{clientId}")
    @Admin
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
        for (String domainName : domains.getAllDomainNames()) {
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

    @Scheduled(fixedDelayString = "PT5S")
    public void ping() {
        for (String domainName : domains.getAllDomainNames()) {
            pushService.send(domains.getDomainNodeId(domainName), "PING " + domainName);
        }
    }

}
