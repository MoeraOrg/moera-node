package org.moera.node.rest;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

import org.moera.node.auth.Scope;
import org.moera.node.data.AskHistoryRepository;
import org.moera.node.domain.Domains;
import org.moera.node.friends.FriendCache;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.LiberinManager;
import org.moera.node.liberin.model.AskSubjectsChangedLiberin;
import org.moera.node.model.AskSubject;
import org.moera.node.model.Features;
import org.moera.node.option.OptionHook;
import org.moera.node.option.OptionValueChange;
import org.moera.node.option.Options;
import org.moera.node.plugin.Plugins;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/features")
@NoCache
public class FeaturesController {

    private static final Logger log = LoggerFactory.getLogger(FeaturesController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private AskHistoryRepository askHistoryRepository;

    @Inject
    private Plugins plugins;

    @Inject
    private FriendCache friendCache;

    @Inject
    private LiberinManager liberinManager;

    @Inject
    private Domains domains;

    @GetMapping
    public Features get() {
        log.info("GET /features");

        return new Features(
                requestContext.getOptions(),
                plugins.getNames(requestContext.nodeId()),
                friendCache.getNodeGroups(),
                friendCache.getClientGroups(requestContext.getClientName(Scope.IDENTIFY)),
                requestContext,
                getAsks(requestContext.getOptions()),
                requestContext.isSubscribedToClient(Scope.IDENTIFY));
    }

    private List<AskSubject> getAsks(Options options) {
        String clientName = requestContext.getClientName(Scope.IDENTIFY);
        if (ObjectUtils.isEmpty(clientName) || requestContext.isOwner()) {
            return null;
        }

        int asksTotal = askHistoryRepository.countByRemoteNode(requestContext.nodeId(), clientName);
        if (asksTotal >= options.getInt("ask.total.max")) {
            return Collections.emptyList();
        }

        Timestamp lastAskSubscribe = askHistoryRepository.findLastCreatedAt(requestContext.nodeId(), clientName,
                AskSubject.SUBSCRIBE);
        Timestamp lastAskFriend = askHistoryRepository.findLastCreatedAt(requestContext.nodeId(), clientName,
                AskSubject.FRIEND);

        List<AskSubject> ask = new ArrayList<>();

        Duration askInterval = options.getDuration("ask.interval").getDuration();
        if (lastAskSubscribe == null || lastAskSubscribe.toInstant().plus(askInterval).isBefore(Instant.now())) {
            ask.add(AskSubject.SUBSCRIBE);
        }
        if (lastAskFriend == null || lastAskFriend.toInstant().plus(askInterval).isBefore(Instant.now())) {
            ask.add(AskSubject.FRIEND);
        }

        return ask;
    }

    @OptionHook({"ask.subscribe.allowed", "ask.friend.allowed", "ask.interval", "ask.total.max"})
    public void askSettingsChanged(OptionValueChange change) {
        liberinManager.send(new AskSubjectsChangedLiberin().withNodeId(change.getNodeId()));
    }

    @Scheduled(fixedDelayString = "P1D")
    public void askHistoryTimeElapsed() {
        domains.getWarmDomainNames().forEach(domainName ->
            liberinManager.send(new AskSubjectsChangedLiberin().withNodeId(domains.getDomainNodeId(domainName)))
        );
    }

}
