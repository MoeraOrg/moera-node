package org.moera.node.rest;

import java.util.UUID;
import javax.inject.Inject;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.data.PushClient;
import org.moera.node.data.PushClientRepository;
import org.moera.node.domain.Domains;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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
    public SseEmitter get(@PathVariable String clientId) throws Throwable {
        log.info("GET /push/{clientId} (clientId = {})", LogUtil.format(clientId));

        if (StringUtils.isEmpty(clientId)) {
            throw new ValidationFailure("push.clientId.blank");
        }

        PushClient client = getClient(clientId);
        updateLastSeenAt(client);

        SseEmitter sseEmitter = new SseEmitter(Long.MAX_VALUE);
        pushService.register(requestContext.nodeId(), client, sseEmitter);
        return sseEmitter;
    }

    private PushClient getClient(String clientId) throws Throwable {
        PushClient client = pushClientRepository.findByClientId(requestContext.nodeId(), clientId).orElse(null);
        if (client != null) {
            return client;
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

    @Scheduled(fixedDelayString = "PT2S")
    public void ping() {
        for (String domainName : domains.getAllDomainNames()) {
            pushService.send(domains.getDomainNodeId(domainName), "PING " + domainName);
        }
    }

}
