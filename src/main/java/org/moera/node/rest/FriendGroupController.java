package org.moera.node.rest;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.moera.lib.node.types.FriendGroupDescription;
import org.moera.lib.node.types.FriendGroupInfo;
import org.moera.lib.node.types.FriendGroupOperations;
import org.moera.lib.node.types.Result;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.auth.AuthenticationException;
import org.moera.node.data.Friend;
import org.moera.node.data.FriendGroup;
import org.moera.node.data.FriendGroupRepository;
import org.moera.node.data.FriendRepository;
import org.moera.node.friends.FriendCache;
import org.moera.node.friends.FriendCachePart;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.model.FriendGroupAddedLiberin;
import org.moera.node.liberin.model.FriendGroupDeletedLiberin;
import org.moera.node.liberin.model.FriendGroupUpdatedLiberin;
import org.moera.node.model.FriendGroupDescriptionUtil;
import org.moera.node.model.FriendGroupInfoUtil;
import org.moera.node.model.ObjectNotFoundFailure;
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
    private FriendRepository friendRepository;

    @Inject
    private FriendCache friendCache;

    @GetMapping
    @Transactional
    public List<FriendGroupInfo> getAll() {
        log.info("GET /people/friends/groups");

        return Arrays.stream(friendCache.getNodeGroups())
                .filter(this::isFriendGroupVisible)
                .map(fg -> FriendGroupInfoUtil.build(fg, requestContext.isAdmin(Scope.VIEW_PEOPLE)))
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

        return FriendGroupInfoUtil.build(friendGroup, requestContext.isAdmin(Scope.VIEW_PEOPLE));
    }

    private boolean isFriendGroupVisible(FriendGroup friendGroup) {
        return requestContext.isAdmin(Scope.VIEW_PEOPLE)
                || friendGroup.getViewPrincipal().isPublic()
                || friendGroup.getViewPrincipal().isPrivate()
                    && requestContext.isMemberOf(friendGroup.getId(), Scope.IDENTIFY);
    }

    @PostMapping
    @Admin(Scope.FRIEND)
    @Transactional
    public FriendGroupInfo post(@RequestBody FriendGroupDescription friendGroupDescription) {
        log.info(
            "POST /people/friends/groups (title = {}, viewPrincipal = {})",
            LogUtil.format(friendGroupDescription.getTitle()),
            LogUtil.format(ObjectUtils.nullSafeToString(
                FriendGroupOperations.getView(friendGroupDescription.getOperations(), null)
            ))
        );

        friendGroupDescription.validate();
        OperationsValidator.validateOperations(
            friendGroupDescription.getOperations(),
            false,
            "friend-group.operations.wrong-principal"
        );

        FriendGroup friendGroup = new FriendGroup();
        friendGroup.setId(UUID.randomUUID());
        friendGroup.setNodeId(requestContext.nodeId());
        FriendGroupDescriptionUtil.toFriendGroup(friendGroupDescription, friendGroup);
        friendGroup = friendGroupRepository.save(friendGroup);

        requestContext.invalidateFriendCache(FriendCachePart.NODE_GROUPS, null);
        requestContext.send(new FriendGroupAddedLiberin(friendGroup));

        return FriendGroupInfoUtil.build(friendGroup, true);
    }

    @PutMapping("/{id}")
    @Admin(Scope.FRIEND)
    @Transactional
    public FriendGroupInfo put(
        @PathVariable UUID id, @RequestBody FriendGroupDescription friendGroupDescription
    ) {
        log.info(
            "PUT /people/friends/groups/{id} (id = {}, title = {}, viewPrincipal = {})",
            LogUtil.format(id),
            LogUtil.format(friendGroupDescription.getTitle()),
            LogUtil.format(ObjectUtils.nullSafeToString(
                FriendGroupOperations.getView(friendGroupDescription.getOperations(), null)
            ))
        );

        OperationsValidator.validateOperations(
            friendGroupDescription.getOperations(),
            false,
            "friend-group.operations.wrong-principal"
        );

        FriendGroup friendGroup = friendGroupRepository.findByNodeIdAndId(requestContext.nodeId(), id)
            .orElseThrow(() -> new ObjectNotFoundFailure("friend-group.not-found"));
        Principal latestViewPrincipal = friendGroup.getViewPrincipal();
        FriendGroupDescriptionUtil.toFriendGroup(friendGroupDescription, friendGroup);

        requestContext.invalidateFriendCache(FriendCachePart.NODE_GROUPS, null);
        requestContext.invalidateFriendCache(FriendCachePart.CLIENT_GROUPS_ALL, null);
        requestContext.send(new FriendGroupUpdatedLiberin(friendGroup, latestViewPrincipal));

        return FriendGroupInfoUtil.build(friendGroup, true);
    }

    @DeleteMapping("/{id}")
    @Admin(Scope.FRIEND)
    @Transactional
    public Result delete(@PathVariable UUID id) {
        log.info("DELETE /people/friends/groups/{id} (id = {})", LogUtil.format(id));

        FriendGroup friendGroup = friendGroupRepository.findByNodeIdAndId(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("friend-group.not-found"));
        Principal latestViewPrincipal = friendGroup.getViewPrincipal();
        List<Friend> members = friendRepository.findAllByNodeIdAndGroup(requestContext.nodeId(), id);
        friendGroupRepository.delete(friendGroup);

        requestContext.invalidateFriendCache(FriendCachePart.NODE_GROUPS, null);
        requestContext.invalidateFriendCache(FriendCachePart.CLIENT_GROUPS_ALL, null);
        // We send a liberin for admin and for every member of the group, because the group will not exist
        // when the liberins will be processed
        requestContext.send(new FriendGroupDeletedLiberin(id, latestViewPrincipal, null));
        members.forEach(member ->
                requestContext.send(new FriendGroupDeletedLiberin(id, latestViewPrincipal, member.getRemoteNodeName())));

        return Result.OK;
    }

}
