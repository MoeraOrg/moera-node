package org.moera.node.rest;

import javax.inject.Inject;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.domain.Domains;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.push.PushService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
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

    @GetMapping("/{clientId}")
    @Admin
    public SseEmitter get(@PathVariable String clientId) {
        log.info("GET /push/{clientId} (clientId = {})", LogUtil.format(clientId));

        SseEmitter sseEmitter = new SseEmitter(Long.MAX_VALUE);
        pushService.register(requestContext.nodeId(), clientId, sseEmitter);
        return sseEmitter;
    }

    @Scheduled(fixedDelayString = "PT2S")
    public void ping() {
        for (String domainName : domains.getAllDomainNames()) {
            pushService.send(domains.getDomainNodeId(domainName), "PING " + domainName);
        }
    }

}
