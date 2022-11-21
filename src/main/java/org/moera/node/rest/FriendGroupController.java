package org.moera.node.rest;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.auth.AuthenticationException;
import org.moera.node.auth.principal.Principal;
import org.moera.node.data.FriendGroup;
import org.moera.node.data.FriendGroupRepository;
import org.moera.node.friends.FriendCache;
import org.moera.node.friends.FriendCachePart;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.model.FriendGroupAddedLiberin;
import org.moera.node.liberin.model.FriendGroupDeletedLiberin;
import org.moera.node.liberin.model.FriendGroupUpdatedLiberin;
import org.moera.node.model.FriendGroupDescription;
import org.moera.node.model.FriendGroupInfo;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.Result;
import org.moera.node.operations.OperationsValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/people/friends/groups")
@NoCache
public class FriendGroupController {

    private static final Logger log = LoggerFactory.getLogger(FriendGroupController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private FriendGroupRepository friendGroupRepository;

    @Inject
    private FriendCache friendCache;

    @GetMapping
    @Transactional
    public List<FriendGroupInfo> getAll() {
        log.info("GET /people/friends/groups");

        return Arrays.stream(friendCache.getNodeGroups())
                .filter(this::isFriendGroupVisible)
                .map(FriendGroupInfo::new)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @Transactional
    public FriendGroupInfo get(@PathVariable UUID id) {
        log.info("GET /people/friends/groups/{id} (id = {})", LogUtil.format(id));

        FriendGroup friendGroup = friendCache.getNodeGroup(id)
                .orElseThrow(() -> new ObjectNotFoundFailure("friend-group.not-found"));
        if (!isFriendGroupVisible(friendGroup)) {
            throw new AuthenticationException();
        }

        return new FriendGroupInfo(friendGroup);
    }

    private boolean isFriendGroupVisible(FriendGroup friendGroup) {
        return requestContext.isAdmin()
                || friendGroup.getViewPrincipal().isPublic()
                || friendGroup.getViewPrincipal().isPrivate() && requestContext.isMemberOf(friendGroup.getId());
    }

    @PostMapping
    @Admin
    @Transactional
    public FriendGroupInfo post(@Valid @RequestBody FriendGroupDescription friendGroupDescription) {
        log.info("POST /people/friends/groups (title = {}, viewPrincipal = {})",
                LogUtil.format(friendGroupDescription.getTitle()),
                LogUtil.format(ObjectUtils.nullSafeToString(friendGroupDescription.getPrincipal("view"))));

        OperationsValidator.validateOperations(friendGroupDescription::getPrincipal,
                OperationsValidator.FRIEND_GROUP_OPERATIONS, false,
                "friendGroupDescription.operations.wrong-principal");

        FriendGroup friendGroup = new FriendGroup();
        friendGroup.setId(UUID.randomUUID());
        friendGroup.setNodeId(requestContext.nodeId());
        friendGroupDescription.toFriendGroup(friendGroup);
        friendGroup = friendGroupRepository.save(friendGroup);

        requestContext.invalidateFriendCache(FriendCachePart.NODE_GROUPS, null);
        requestContext.send(new FriendGroupAddedLiberin(friendGroup));

        return new FriendGroupInfo(friendGroup);
    }

    @PutMapping("/{id}")
    @Admin
    @Transactional
    public FriendGroupInfo put(@PathVariable UUID id,
                               @Valid @RequestBody FriendGroupDescription friendGroupDescription) {
        log.info("PUT /people/friends/groups/{id} (id = {}, title = {}, viewPrincipal = {})",
                LogUtil.format(id), LogUtil.format(friendGroupDescription.getTitle()),
                LogUtil.format(ObjectUtils.nullSafeToString(friendGroupDescription.getPrincipal("view"))));

        OperationsValidator.validateOperations(friendGroupDescription::getPrincipal,
                OperationsValidator.FRIEND_GROUP_OPERATIONS, false,
                "friendGroupDescription.operations.wrong-principal");

        FriendGroup friendGroup = friendGroupRepository.findByNodeIdAndId(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("friend-group.not-found"));
        Principal latestViewPrincipal = friendGroup.getViewPrincipal();
        friendGroupDescription.toFriendGroup(friendGroup);

        requestContext.invalidateFriendCache(FriendCachePart.NODE_GROUPS, null);
        requestContext.invalidateFriendCache(FriendCachePart.CLIENT_GROUPS_ALL, null);
        requestContext.send(new FriendGroupUpdatedLiberin(friendGroup, latestViewPrincipal));

        return new FriendGroupInfo(friendGroup);
    }

    @DeleteMapping("/{id}")
    @Admin
    @Transactional
    public Result delete(@PathVariable UUID id) {
        log.info("DELETE /people/friends/groups/{id} (id = {})", LogUtil.format(id));

        FriendGroup friendGroup = friendGroupRepository.findByNodeIdAndId(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("friend-group.not-found"));
        Principal latestViewPrincipal = friendGroup.getViewPrincipal();
        friendGroupRepository.delete(friendGroup);

        requestContext.invalidateFriendCache(FriendCachePart.NODE_GROUPS, null);
        requestContext.invalidateFriendCache(FriendCachePart.CLIENT_GROUPS_ALL, null);
        requestContext.send(new FriendGroupDeletedLiberin(id, latestViewPrincipal));

        return Result.OK;
    }

}
