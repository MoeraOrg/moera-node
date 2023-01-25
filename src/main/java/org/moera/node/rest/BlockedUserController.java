package org.moera.node.rest;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.auth.AuthenticationException;
import org.moera.node.data.BlockedUser;
import org.moera.node.data.BlockedUserRepository;
import org.moera.node.data.Entry;
import org.moera.node.data.EntryRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.model.BlockedUserAttributes;
import org.moera.node.model.BlockedUserFilter;
import org.moera.node.model.BlockedUserInfo;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.Result;
import org.moera.node.model.ValidationFailure;
import org.moera.node.operations.BlockedUserOperations;
import org.moera.node.operations.ContactOperations;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/blocked-users")
@NoCache
public class BlockedUserController {

    private static final Logger log = LoggerFactory.getLogger(BlockedUserController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private BlockedUserRepository blockedUserRepository;

    @Inject
    private EntryRepository entryRepository;

    @Inject
    private BlockedUserOperations blockedUserOperations;

    @Inject
    private ContactOperations contactOperations;

    @PostMapping
    @Admin
    @Transactional
    public ResponseEntity<BlockedUserInfo> post(
            @Valid @RequestBody BlockedUserAttributes blockedUserAttributes) {
        log.info("POST /blocked-instants (blockedOperation = {}, nodeName = {}, entryId = {}, entryNodeName = {}"
                        + " entryPostingId = {})",
                LogUtil.format(blockedUserAttributes.getBlockedOperation().toString()),
                LogUtil.format(blockedUserAttributes.getNodeName()),
                LogUtil.format(blockedUserAttributes.getEntryId()),
                LogUtil.format(blockedUserAttributes.getEntryNodeName()),
                LogUtil.format(blockedUserAttributes.getEntryPostingId()));

        if (blockedUserAttributes.getBlockedOperation() == null) {
            throw new ValidationFailure("blockedUserAttributes.blockedOperation.blank");
        }

        Entry entry = null;
        if (blockedUserAttributes.getEntryId() != null) {
            entry = entryRepository.findByNodeIdAndId(requestContext.nodeId(), blockedUserAttributes.getEntryId())
                    .orElseThrow(() -> new ObjectNotFoundFailure("entry.not-found"));
        }

        Collection<BlockedUser> blockedUsers = blockedUserOperations.findExact(requestContext.nodeId(),
                blockedUserAttributes.getBlockedOperation(), blockedUserAttributes.getNodeName(),
                blockedUserAttributes.getEntryId(), blockedUserAttributes.getEntryNodeName(),
                blockedUserAttributes.getEntryPostingId());
        blockedUsers.forEach(blockedUser -> {
            blockedUserRepository.delete(blockedUser);
            contactOperations.updateBlockedUserCounts(blockedUser, -1);
        });

        BlockedUser blockedUser = new BlockedUser();
        blockedUser.setId(UUID.randomUUID());
        blockedUser.setNodeId(requestContext.nodeId());
        blockedUser.setEntry(entry);
        blockedUser.setCreatedAt(Util.now());
        blockedUserAttributes.toBlockedInstant(blockedUser);
        blockedUser = blockedUserRepository.save(blockedUser);

        if (blockedUser.isGlobal() && blockedUser.getDeadline() == null) {
            contactOperations.assignCloseness(blockedUser.getRemoteNodeName(), 0);
        }
        contactOperations.updateBlockedUserCounts(blockedUser, 1);
        contactOperations.updateViewPrincipal(blockedUser).fill(blockedUser);

//        requestContext.send(new BlockedInstantAddedLiberin(blockedInstant));

        return ResponseEntity.created(URI.create("/blocked-users/" + blockedUser.getId()))
                .body(new BlockedUserInfo(blockedUser));
    }

    @GetMapping("/{id}")
    @Transactional
    public BlockedUserInfo get(@PathVariable UUID id) {
        log.info("GET /blocked-users/{id}, (id = {})", LogUtil.format(id));

        BlockedUser blockedUser = blockedUserRepository.findByNodeIdAndId(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("blocked-user.not-found"));

        if (!requestContext.isPrincipal(BlockedUser.getViewAllE(requestContext.getOptions()))
                && !requestContext.isClient(blockedUser.getRemoteNodeName())) {
            throw new AuthenticationException();
        }

        return new BlockedUserInfo(blockedUser);
    }

    @DeleteMapping("/{id}")
    @Admin
    @Transactional
    public Result delete(@PathVariable UUID id) {
        log.info("DELETE /blocked-instants/{id}, (id = {})", LogUtil.format(id));

        BlockedUser blockedUser = blockedUserRepository.findByNodeIdAndId(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("blocked-user.not-found"));
        blockedUserRepository.delete(blockedUser);

//        requestContext.send(new BlockedInstantDeletedLiberin(blockedInstant));

        return Result.OK;
    }

    @PostMapping("/search")
    @Transactional
    public List<BlockedUserInfo> post(@Valid @RequestBody BlockedUserFilter blockedUserFilter) {
        log.info("POST /blocked-users/search");

        if (!requestContext.isPrincipal(BlockedUser.getViewAllE(requestContext.getOptions()))
                && (blockedUserFilter.getNodeName() == null
                    || !requestContext.isClient(blockedUserFilter.getNodeName()))) {
            throw new AuthenticationException();
        }

        return blockedUserOperations.search(requestContext.nodeId(), blockedUserFilter.getBlockedOperation(),
                        blockedUserFilter.getNodeName(), blockedUserFilter.getEntryId(),
                        blockedUserFilter.getEntryNodeName(), blockedUserFilter.getEntryPostingId())
                .map(BlockedUserInfo::new)
                .collect(Collectors.toList());
    }

}
