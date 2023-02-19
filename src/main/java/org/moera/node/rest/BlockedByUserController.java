package org.moera.node.rest;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.AuthenticationException;
import org.moera.node.data.BlockedByUser;
import org.moera.node.data.BlockedByUserRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.model.BlockedByUserFilter;
import org.moera.node.model.BlockedByUserInfo;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.ValidationFailure;
import org.moera.node.operations.BlockedByUserOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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

    @GetMapping
    @Transactional
    public List<BlockedByUserInfo> getAll(@RequestParam(required = false) String nodeName,
                                          @RequestParam(required = false) String postingId) {
        log.info("GET /people/blocked-by-users (nodeName = {}, postingId = {})",
                LogUtil.format(nodeName), LogUtil.format(postingId));

        if (!requestContext.isPrincipal(BlockedByUser.getViewAllE(requestContext.getOptions()))) {
            throw new AuthenticationException();
        }

        Collection<BlockedByUser> blockedByUsers;
        if (nodeName == null) {
            if (postingId == null) {
                blockedByUsers = blockedByUserRepository.findByNodeIdGlobal(requestContext.nodeId());
            } else {
                throw new ValidationFailure("blocked-by-user.nodeName.blank");
            }
        } else {
            if (postingId == null) {
                blockedByUsers = blockedByUserRepository.findByRemoteNode(requestContext.nodeId(), nodeName);
            } else {
                blockedByUsers = blockedByUserRepository.findByRemotePosting(
                        requestContext.nodeId(), nodeName, postingId);
            }
        }

        return blockedByUsers.stream()
                .map(bbu -> new BlockedByUserInfo(bbu, requestContext.getOptions(), requestContext))
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @Transactional
    public BlockedByUserInfo get(@PathVariable UUID id) {
        log.info("GET /people/blocked-by-users/{id}, (id = {})", LogUtil.format(id));

        BlockedByUser blockedByUser = blockedByUserRepository.findByNodeIdAndId(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("blocked-by-user.not-found"));

        if (!requestContext.isPrincipal(BlockedByUser.getViewAllE(requestContext.getOptions()))
                && !requestContext.isClient(blockedByUser.getRemoteNodeName())) {
            throw new AuthenticationException();
        }

        return new BlockedByUserInfo(blockedByUser, requestContext.getOptions(), requestContext);
    }

    @PostMapping("/search")
    @Transactional
    public List<BlockedByUserInfo> search(@Valid @RequestBody BlockedByUserFilter blockedByUserFilter) {
        log.info("POST /people/blocked-by-users/search");

        if (!requestContext.isPrincipal(BlockedByUser.getViewAllE(requestContext.getOptions()))) {
            throw new AuthenticationException();
        }

        return blockedByUserOperations.search(requestContext.nodeId(), blockedByUserFilter.getBlockedOperations(),
                        blockedByUserFilter.getPostings()).stream()
                .map(bbu -> new BlockedByUserInfo(bbu, requestContext.getOptions(), requestContext))
                .collect(Collectors.toList());
    }

}
