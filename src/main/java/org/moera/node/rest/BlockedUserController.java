package org.moera.node.rest;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.moera.lib.node.types.BlockedUserAttributes;
import org.moera.lib.node.types.BlockedUserFilter;
import org.moera.lib.node.types.BlockedUserInfo;
import org.moera.lib.node.types.BlockedUsersChecksums;
import org.moera.lib.node.types.Result;
import org.moera.lib.node.types.Scope;
import org.moera.lib.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.auth.AuthenticationException;
import org.moera.node.data.BlockedUser;
import org.moera.node.data.BlockedUserRepository;
import org.moera.node.data.Entry;
import org.moera.node.data.EntryRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.model.BlockedUserAddedLiberin;
import org.moera.node.liberin.model.BlockedUserDeletedLiberin;
import org.moera.node.model.BlockedUserAttributesUtil;
import org.moera.node.model.BlockedUserInfoUtil;
import org.moera.node.model.BlockedUsersChecksumsUtil;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.operations.BlockedUserOperations;
import org.moera.node.operations.ContactOperations;
import org.moera.node.text.TextConverter;
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
@RequestMapping("/moera/api/people/blocked-users")
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

    @Inject
    private TextConverter textConverter;

    @PostMapping
    @Admin(Scope.BLOCK)
    @Transactional
    public ResponseEntity<BlockedUserInfo> post(@RequestBody BlockedUserAttributes blockedUserAttributes) {
        log.info(
            "POST /people/blocked-users (blockedOperation = {}, nodeName = {}, entryId = {}, entryNodeName = {}"
                + " entryPostingId = {})",
            LogUtil.format(blockedUserAttributes.getBlockedOperation().toString()),
            LogUtil.format(blockedUserAttributes.getNodeName()),
            LogUtil.format(blockedUserAttributes.getEntryId()),
            LogUtil.format(blockedUserAttributes.getEntryNodeName()),
            LogUtil.format(blockedUserAttributes.getEntryPostingId())
        );

        blockedUserAttributes.validate();

        Entry entry = null;
        if (blockedUserAttributes.getEntryId() != null) {
            UUID entryId = Util.uuid(blockedUserAttributes.getEntryId())
                .orElseThrow(() -> new ObjectNotFoundFailure("entry.not-found"));
            entry = entryRepository.findByNodeIdAndId(requestContext.nodeId(), entryId)
                .orElseThrow(() -> new ObjectNotFoundFailure("entry.not-found"));
        }

        BlockedUser blockedUser = new BlockedUser();
        blockedUser.setId(UUID.randomUUID());
        blockedUser.setNodeId(requestContext.nodeId());
        blockedUser.setEntry(entry);
        blockedUser.setCreatedAt(Util.now());
        BlockedUserAttributesUtil.toBlockedUser(blockedUserAttributes, blockedUser, textConverter);
        blockedUser = blockedUserRepository.save(blockedUser);

        if (blockedUser.isGlobal() && blockedUser.getDeadline() == null) {
            // will get this value anyway after recalculation, but immediate change gives a better user experience
            contactOperations.assignDistance(blockedUser.getRemoteNodeName(), 4);
        }
        contactOperations.updateBlockedUserCounts(blockedUser, 1);
        contactOperations.updateViewPrincipal(blockedUser).fill(blockedUser);

        requestContext.blockedUsersUpdated();
        requestContext.send(new BlockedUserAddedLiberin(blockedUser));

        return ResponseEntity.created(URI.create("/blocked-users/" + blockedUser.getId()))
            .body(BlockedUserInfoUtil.build(blockedUser, requestContext.getOptions(), requestContext));
    }

    @GetMapping("/{id}")
    @Transactional
    public BlockedUserInfo get(@PathVariable UUID id) {
        log.info("GET /people/blocked-users/{id}, (id = {})", LogUtil.format(id));

        BlockedUser blockedUser = blockedUserRepository.findByNodeIdAndId(requestContext.nodeId(), id)
            .orElseThrow(() -> new ObjectNotFoundFailure("blocked-user.not-found"));

        if (
            !requestContext.isPrincipal(BlockedUser.getViewAllE(requestContext.getOptions()), Scope.VIEW_PEOPLE)
            && !requestContext.isClient(blockedUser.getRemoteNodeName(), Scope.VIEW_PEOPLE)
        ) {
            throw new AuthenticationException();
        }

        return BlockedUserInfoUtil.build(blockedUser, requestContext.getOptions(), requestContext);
    }

    @DeleteMapping("/{id}")
    @Admin(Scope.BLOCK)
    @Transactional
    public Result delete(@PathVariable UUID id) {
        log.info("DELETE /people/blocked-users/{id}, (id = {})", LogUtil.format(id));

        BlockedUser blockedUser = blockedUserRepository.findByNodeIdAndId(requestContext.nodeId(), id)
            .orElseThrow(() -> new ObjectNotFoundFailure("blocked-user.not-found"));
        blockedUserRepository.delete(blockedUser);
        contactOperations.updateBlockedUserCounts(blockedUser, -1).fill(blockedUser);

        requestContext.blockedUsersUpdated();
        requestContext.send(new BlockedUserDeletedLiberin(blockedUser));

        return Result.OK;
    }

    @PostMapping("/search")
    @Transactional
    public List<BlockedUserInfo> search(@RequestBody BlockedUserFilter blockedUserFilter) {
        log.info("POST /people/blocked-users/search");

        if (
            !requestContext.isPrincipal(BlockedUser.getViewAllE(requestContext.getOptions()), Scope.VIEW_PEOPLE)
            && (
                blockedUserFilter.getNodeName() == null
                || !requestContext.isClient(blockedUserFilter.getNodeName(), Scope.VIEW_PEOPLE)
            )
        ) {
            throw new AuthenticationException();
        }

        return blockedUserOperations.search(
            requestContext.nodeId(),
            blockedUserFilter.getBlockedOperations(),
            blockedUserFilter.getNodeName(),
            Util.uuid(blockedUserFilter.getEntryId()).orElse(null),
            blockedUserFilter.getEntryNodeName(),
            blockedUserFilter.getEntryPostingId(),
            blockedUserFilter.getStrict() != null && blockedUserFilter.getStrict()
        )
            .stream()
            .map(bu -> BlockedUserInfoUtil.build(bu, requestContext.getOptions(), requestContext))
            .collect(Collectors.toList());
    }

    @GetMapping("/checksums")
    @Admin(Scope.OTHER)
    @Transactional
    public BlockedUsersChecksums checksums() {
        log.info("GET /people/blocked-users/checksums");

        return BlockedUsersChecksumsUtil.build(
            requestContext.getOptions().getLong("blocked-users.visibility.checksum")
        );
    }

}
