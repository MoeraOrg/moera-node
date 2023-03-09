package org.moera.node.rest;

import java.net.URI;
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
import org.moera.node.liberin.model.BlockedUserAddedLiberin;
import org.moera.node.liberin.model.BlockedUserDeletedLiberin;
import org.moera.node.model.BlockedUserAttributes;
import org.moera.node.model.BlockedUserFilter;
import org.moera.node.model.BlockedUserInfo;
import org.moera.node.model.BlockedUsersChecksums;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.Result;
import org.moera.node.model.ValidationFailure;
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
    @Admin
    @Transactional
    public ResponseEntity<BlockedUserInfo> post(
            @Valid @RequestBody BlockedUserAttributes blockedUserAttributes) {
        log.info("POST /people/blocked-users (blockedOperation = {}, nodeName = {}, entryId = {}, entryNodeName = {}"
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

        BlockedUser blockedUser = new BlockedUser();
        blockedUser.setId(UUID.randomUUID());
        blockedUser.setNodeId(requestContext.nodeId());
        blockedUser.setEntry(entry);
        blockedUser.setCreatedAt(Util.now());
        blockedUserAttributes.toBlockedInstant(blockedUser, textConverter);
        blockedUser = blockedUserRepository.save(blockedUser);

        if (blockedUser.isGlobal() && blockedUser.getDeadline() == null) {
            contactOperations.assignCloseness(blockedUser.getRemoteNodeName(), 0);
        }
        contactOperations.updateBlockedUserCounts(blockedUser, 1);
        contactOperations.updateViewPrincipal(blockedUser).fill(blockedUser);

        requestContext.blockedUsersUpdated();
        requestContext.send(new BlockedUserAddedLiberin(blockedUser));

        return ResponseEntity.created(URI.create("/blocked-users/" + blockedUser.getId()))
                .body(new BlockedUserInfo(blockedUser, requestContext.getOptions(), requestContext));
    }

    @GetMapping("/{id}")
    @Transactional
    public BlockedUserInfo get(@PathVariable UUID id) {
        log.info("GET /people/blocked-users/{id}, (id = {})", LogUtil.format(id));

        BlockedUser blockedUser = blockedUserRepository.findByNodeIdAndId(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("blocked-user.not-found"));

        if (!requestContext.isPrincipal(BlockedUser.getViewAllE(requestContext.getOptions()))
                && !requestContext.isClient(blockedUser.getRemoteNodeName())) {
            throw new AuthenticationException();
        }

        return new BlockedUserInfo(blockedUser, requestContext.getOptions(), requestContext);
    }

    @DeleteMapping("/{id}")
    @Admin
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
    public List<BlockedUserInfo> search(@Valid @RequestBody BlockedUserFilter blockedUserFilter) {
        log.info("POST /people/blocked-users/search");

        if (!requestContext.isPrincipal(BlockedUser.getViewAllE(requestContext.getOptions()))
                && (blockedUserFilter.getNodeName() == null
                    || !requestContext.isClient(blockedUserFilter.getNodeName()))) {
            throw new AuthenticationException();
        }

        return blockedUserOperations.search(requestContext.nodeId(), blockedUserFilter.getBlockedOperations(),
                        blockedUserFilter.getNodeName(), blockedUserFilter.getEntryId(),
                        blockedUserFilter.getEntryNodeName(), blockedUserFilter.getEntryPostingId(),
                        blockedUserFilter.getStrict() != null && blockedUserFilter.getStrict()).stream()
                .map(bu -> new BlockedUserInfo(bu, requestContext.getOptions(), requestContext))
                .collect(Collectors.toList());
    }

    @GetMapping("/checksums")
    @Admin
    @Transactional
    public BlockedUsersChecksums checksums() {
        log.info("GET /people/blocked-users/checksums");

        return new BlockedUsersChecksums(requestContext.getOptions().getLong("blocked-users.visibility.checksum"));
    }

}
