package org.moera.node.rest;

import java.util.UUID;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.moera.lib.node.types.RemotePostingVerificationInfo;
import org.moera.lib.node.types.RemoteReactionVerificationInfo;
import org.moera.lib.node.types.Scope;
import org.moera.lib.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.data.RemotePostingVerification;
import org.moera.node.data.RemotePostingVerificationRepository;
import org.moera.node.data.RemoteReactionVerification;
import org.moera.node.data.RemoteReactionVerificationRepository;
import org.moera.node.data.RemoteVerificationRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.global.RequestCounter;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.RemotePostingVerificationInfoUtil;
import org.moera.node.model.RemoteReactionVerificationInfoUtil;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/async-operations")
@NoCache
public class AsyncOperationController {

    private static final Logger log = LoggerFactory.getLogger(AsyncOperationController.class);

    @Inject
    private RequestCounter requestCounter;

    @Inject
    private RequestContext requestContext;

    @Inject
    private RemoteVerificationRepository remoteVerificationRepository;

    @Inject
    private RemotePostingVerificationRepository remotePostingVerificationRepository;

    @Inject
    private RemoteReactionVerificationRepository remoteReactionVerificationRepository;

    @GetMapping("/remote-posting-verification/{id}")
    @Admin(Scope.OTHER)
    @Transactional
    public RemotePostingVerificationInfo getRemotePostingVerification(@PathVariable UUID id) {
        log.info("GET /async-operations/remote-posting-verification/{id}, (id = {})", LogUtil.format(id));

        RemotePostingVerification data =
                remotePostingVerificationRepository.findByNodeIdAndId(requestContext.nodeId(), id)
                        .orElseThrow(() -> new ObjectNotFoundFailure("async-operation.not-found"));

        return RemotePostingVerificationInfoUtil.build(data);
    }

    @GetMapping("/remote-reaction-verification/{id}")
    @Admin(Scope.OTHER)
    @Transactional
    public RemoteReactionVerificationInfo getRemoteReactionVerification(@PathVariable UUID id) {
        log.info("GET /async-operations/remote-reaction-verification/{id}, (id = {})", LogUtil.format(id));

        RemoteReactionVerification data =
                remoteReactionVerificationRepository.findByNodeIdAndId(requestContext.nodeId(), id)
                        .orElseThrow(() -> new ObjectNotFoundFailure("async-operation.not-found"));

        return RemoteReactionVerificationInfoUtil.build(data);
    }

    @Scheduled(fixedDelayString = "PT30M")
    @Transactional
    public void purgeExpiredVerifications() {
        try (var ignored = requestCounter.allot()) {
            log.info("Purging expired deleted verifications");

            remoteVerificationRepository.deleteExpired(Util.now());
        }
    }

}
