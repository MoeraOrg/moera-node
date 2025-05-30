package org.moera.node.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.moera.lib.node.types.ContactInfo;
import org.moera.lib.node.types.FriendDescription;
import org.moera.lib.node.types.FriendGroupAssignment;
import org.moera.lib.node.types.FriendGroupDetails;
import org.moera.lib.node.types.FriendInfo;
import org.moera.lib.node.types.Scope;
import org.moera.lib.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.auth.AuthenticationException;
import org.moera.node.data.Contact;
import org.moera.node.data.ContactRepository;
import org.moera.node.data.Friend;
import org.moera.node.data.FriendGroup;
import org.moera.node.data.FriendGroupRepository;
import org.moera.node.data.FriendRepository;
import org.moera.node.friends.FriendCache;
import org.moera.node.friends.FriendCachePart;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.model.FriendshipUpdatedLiberin;
import org.moera.node.model.ContactInfoUtil;
import org.moera.node.model.FriendDescriptionUtil;
import org.moera.node.model.FriendGroupAssignmentUtil;
import org.moera.node.model.FriendGroupDetailsUtil;
import org.moera.node.model.FriendInfoUtil;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.operations.ContactOperations;
import org.moera.node.operations.OperationsValidator;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@ApiController
@RequestMapping("/moera/api/people/friends")
@NoCache
public class FriendController {

    private static final Logger log = LoggerFactory.getLogger(FriendController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private FriendGroupRepository friendGroupRepository;

    @Inject
    private FriendRepository friendRepository;

    @Inject
    private FriendCache friendCache;

    @Inject
    private ContactOperations contactOperations;

    @Inject
    private ContactRepository contactRepository;

    @GetMapping
    @Transactional
    public List<FriendInfo> getAll(@RequestParam(required = false, name = "group") UUID groupId) {
        log.info("GET /people/friends (group = {})", LogUtil.format(groupId));

        if (!requestContext.isPrincipal(Friend.getViewAllE(requestContext.getOptions()), Scope.VIEW_PEOPLE)) {
            throw new AuthenticationException();
        }

        if (groupId != null) {
            FriendGroup group = friendCache.getNodeGroup(groupId)
                .orElseThrow(() -> new ObjectNotFoundFailure("friend-group.not-found"));
            if (!requestContext.isAdmin(Scope.VIEW_PEOPLE) && !group.getViewPrincipal().isPublic()) {
                throw new AuthenticationException();
            }
        }

        List<Friend> friends = groupId == null
            ? friendRepository.findAllByNodeId(requestContext.nodeId())
            : friendRepository.findAllByNodeIdAndGroup(requestContext.nodeId(), groupId);
        List<FriendInfo> friendInfos = new ArrayList<>();
        List<FriendGroupDetails> groups = null;
        String prevNodeName = null;
        for (Friend friend : friends) {
            if (prevNodeName != null && !prevNodeName.equals(friend.getRemoteNodeName())) {
                groups = null;
            }
            if (groups == null) {
                FriendInfo info = FriendInfoUtil.build(friend, requestContext.getOptions(), requestContext);
                if (groupId == null) {
                    groups = new ArrayList<>();
                    info.setGroups(groups);
                }
                friendInfos.add(info);
                prevNodeName = friend.getRemoteNodeName();
            }
            boolean visible =
                requestContext.isPrincipal(friend.getViewE(), Scope.VIEW_PEOPLE)
                && (
                    requestContext.isAdmin(Scope.VIEW_PEOPLE)
                    || requestContext.isClient(friend.getRemoteNodeName(), Scope.VIEW_PEOPLE)
                    || friend.getFriendGroup().getViewPrincipal().isPublic()
                );
            if (groups != null && visible) {
                groups.add(FriendGroupDetailsUtil.build(friend, requestContext.isAdmin(Scope.VIEW_PEOPLE)));
            }
        }

        return friendInfos.stream().filter(fi -> !fi.getGroups().isEmpty()).collect(Collectors.toList());
    }

    @GetMapping("/{name}")
    @Transactional
    public FriendInfo get(@PathVariable("name") String nodeName) {
        log.info("GET /people/friends/{name} (name = {})", LogUtil.format(nodeName));

        if (
            !requestContext.isPrincipal(Friend.getViewAllE(requestContext.getOptions()), Scope.VIEW_PEOPLE)
            && !requestContext.isClient(nodeName, Scope.VIEW_PEOPLE)
        ) {
            throw new AuthenticationException();
        }

        ContactInfo contact = contactRepository.findByRemoteNode(requestContext.nodeId(), nodeName)
            .map(c -> ContactInfoUtil.build(c, requestContext.getOptions(), requestContext))
            .orElse(null);

        boolean privileged =
            requestContext.isAdmin(Scope.VIEW_PEOPLE)
            || requestContext.isClient(nodeName, Scope.VIEW_PEOPLE);
        Map<UUID, Boolean> isPublic = Arrays.stream(friendCache.getNodeGroups())
            .collect(Collectors.toMap(FriendGroup::getId, fg -> fg.getViewPrincipal().isPublic()));
        List<FriendGroupDetails> groups = Arrays.stream(friendCache.getClientGroups(nodeName))
            .filter(fr -> isPublic.get(fr.getFriendGroup().getId()) || privileged)
            .filter(fr -> requestContext.isPrincipal(fr.getViewE(), Scope.VIEW_PEOPLE))
            .map(fr -> FriendGroupDetailsUtil.build(fr, requestContext.isAdmin(Scope.VIEW_PEOPLE)))
            .collect(Collectors.toList());

        return FriendInfoUtil.build(nodeName, contact, groups);
    }

    @PutMapping
    @Admin(Scope.FRIEND)
    @Transactional
    public List<FriendInfo> put(@RequestBody FriendDescription[] friendDescriptions) {
        log.info("PUT /people/friends");

        List<FriendInfo> result = new ArrayList<>();

        Map<UUID, FriendGroup> groups = new HashMap<>();
        for (FriendDescription friendDescription : friendDescriptions) {
            friendDescription.validate();

            Contact contact = contactOperations.find(friendDescription.getNodeName());
            FriendInfo friendInfo = FriendInfoUtil.build(contact, requestContext.getOptions(), requestContext);
            result.add(friendInfo);

            Map<UUID, Pair<FriendGroupAssignment, Friend>> targetGroups = new HashMap<>();
            if (friendDescription.getGroups() != null) {
                for (var ga : friendDescription.getGroups()) {
                    OperationsValidator.validateOperations(
                        ga.getOperations(),
                        false,
                        "friend.operations.wrong-principal"
                    );
                    Util.uuid(ga.getId()).ifPresent(
                        groupId -> targetGroups.put(groupId, Pair.of(ga, new Friend()))
                    );
                }
            }

            List<Friend> friends = new ArrayList<>(
                friendRepository.findAllByNodeIdAndName(requestContext.nodeId(), friendDescription.getNodeName())
            );
            for (Friend friend : friends) {
                var target = targetGroups.get(friend.getFriendGroup().getId());
                if (target != null) {
                    targetGroups.put(friend.getFriendGroup().getId(), Pair.of(target.getFirst(), friend));
                } else {
                    friendRepository.delete(friend);
                    contact = contactOperations.updateFriendCount(friend.getRemoteNodeName(), -1);
                }
            }

            for (var target : targetGroups.entrySet()) {
                Friend friend = target.getValue().getSecond();
                if (friend.getId() == null) {
                    friend.setId(UUID.randomUUID());
                    friend.setNodeId(requestContext.nodeId());
                    FriendDescriptionUtil.toFriend(friendDescription, friend);
                    FriendGroup group = groups.computeIfAbsent(
                        target.getKey(),
                        id -> friendGroupRepository.findByNodeIdAndId(requestContext.nodeId(), id)
                            .orElseThrow(() -> new ObjectNotFoundFailure("friend-group.not-found"))
                    );
                    friend.setFriendGroup(group);
                    FriendGroupAssignmentUtil.toFriend(target.getValue().getFirst(), friend);
                    friend = friendRepository.save(friend);

                    contactOperations.updateFriendCount(friend.getRemoteNodeName(), 1);
                } else {
                    FriendGroupAssignmentUtil.toFriend(target.getValue().getFirst(), friend);
                }
                contact = contactOperations.updateViewPrincipal(friend);
                contact.fill(friend);
                if (friendInfo.getGroups() == null) {
                    friendInfo.setGroups(new ArrayList<>());
                }
                friendInfo.getGroups().add(FriendGroupDetailsUtil.build(friend, true));
            }

            friendInfo.setContact(ContactInfoUtil.build(contact, requestContext.getOptions(), requestContext));

            requestContext.invalidateFriendCache(FriendCachePart.CLIENT_GROUPS, friendDescription.getNodeName());
            requestContext.send(
                new FriendshipUpdatedLiberin(friendDescription.getNodeName(), friendInfo.getGroups(), contact)
            );
        }

        return result;
    }

}
