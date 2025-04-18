package org.moera.node.rest;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.moera.lib.node.types.BlockedByUserFilter;
import org.moera.lib.node.types.BlockedByUserInfo;
import org.moera.lib.node.types.Scope;
import org.moera.lib.util.LogUtil;
import org.moera.node.auth.AuthenticationException;
import org.moera.node.data.BlockedByUser;
import org.moera.node.data.BlockedByUserRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.model.BlockedByUserInfoUtil;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.operations.BlockedByUserOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/people/blocked-by-users")
@NoCache
public class BlockedByUserController {

    private static final Logger log = LoggerFactory.getLogger(BlockedByUserController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private BlockedByUserRepository blockedByUserRepository;

    @Inject
    private BlockedByUserOperations blockedByUserOperations;

    @GetMapping("/{id}")
    @Transactional
    public BlockedByUserInfo get(@PathVariable UUID id) {
        log.info("GET /people/blocked-by-users/{id}, (id = {})", LogUtil.format(id));

        BlockedByUser blockedByUser = blockedByUserRepository.findByNodeIdAndId(requestContext.nodeId(), id)
            .orElseThrow(() -> new ObjectNotFoundFailure("blocked-by-user.not-found"));

        if (
            !requestContext.isPrincipal(BlockedByUser.getViewAllE(requestContext.getOptions()), Scope.VIEW_PEOPLE)
            && !requestContext.isClient(blockedByUser.getRemoteNodeName(), Scope.VIEW_PEOPLE)
        ) {
            throw new AuthenticationException();
        }

        return BlockedByUserInfoUtil.build(blockedByUser, requestContext.getOptions(), requestContext);
    }

    @PostMapping("/search")
    @Transactional
    public List<BlockedByUserInfo> search(@RequestBody BlockedByUserFilter blockedByUserFilter) {
        log.info("POST /people/blocked-by-users/search");

        if (!requestContext.isPrincipal(BlockedByUser.getViewAllE(requestContext.getOptions()), Scope.VIEW_PEOPLE)) {
            throw new AuthenticationException();
        }

        return blockedByUserOperations.search(
            requestContext.nodeId(),
            blockedByUserFilter.getBlockedOperations(),
            blockedByUserFilter.getPostings(),
            blockedByUserFilter.getStrict() != null && blockedByUserFilter.getStrict()
        )
            .stream()
            .map(bbu -> BlockedByUserInfoUtil.build(bbu, requestContext.getOptions(), requestContext))
            .collect(Collectors.toList());
    }

}
