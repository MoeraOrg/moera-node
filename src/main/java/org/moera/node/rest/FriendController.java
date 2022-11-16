package org.moera.node.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.data.Friend;
import org.moera.node.data.FriendGroup;
import org.moera.node.data.FriendGroupRepository;
import org.moera.node.data.FriendRepository;
import org.moera.node.friends.FriendCache;
import org.moera.node.friends.FriendCachePart;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.model.FeaturesUpdatedLiberin;
import org.moera.node.liberin.model.FriendshipUpdatedLiberin;
import org.moera.node.model.FriendDescription;
import org.moera.node.model.FriendGroupDetails;
import org.moera.node.model.FriendInfo;
import org.moera.node.model.ObjectNotFoundFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    @GetMapping
    @Transactional
    public List<FriendInfo> getAll(@RequestParam(required = false, name = "group") UUID groupId) {
        log.info("GET /people/friends (group = {})", LogUtil.format(groupId));

        if (groupId != null) {
            FriendGroup group = friendCache.getNodeGroup(groupId)
                    .orElseThrow(() -> new ObjectNotFoundFailure("friend-group.not-found"));
            if (!requestContext.isAdmin() && !group.isVisible()) {
                throw new ObjectNotFoundFailure("friend-group.not-found");
            }
        }

        List<Friend> friends = groupId == null
                ? friendRepository.findAllByNodeId(requestContext.nodeId())
                : friendRepository.findAllByNodeIdAndGroup(requestContext.nodeId(), groupId);
        List<FriendInfo> friendInfos = new ArrayList<>();
        List<FriendGroupDetails> groups = null;
        UUID prevId = null;
        for (Friend friend : friends) {
            if (prevId != null && !prevId.equals(friend.getId())) {
                groups = null;
            }
            if (groups == null) {
                FriendInfo info = new FriendInfo();
                info.setNodeName(friend.getNodeName());
                if (groupId == null) {
                    groups = new ArrayList<>();
                    info.setGroups(groups);
                }
                friendInfos.add(info);
                prevId = friend.getId();
            }
            boolean visible = requestContext.isAdmin() || requestContext.isClient(friend.getNodeName())
                    || friend.getFriendGroup().isVisible();
            if (groups != null && visible) {
                groups.add(new FriendGroupDetails(friend));
            }
        }

        return friendInfos;
    }

    @GetMapping("/{name}")
    @Transactional
    public FriendInfo get(@PathVariable("name") String nodeName) {
        log.info("GET /people/friends/{name} (name = {})", LogUtil.format(nodeName));

        boolean privileged = requestContext.isAdmin() || requestContext.isClient(nodeName);
        Map<String, Boolean> visible = Arrays.stream(friendCache.getNodeGroups())
                .collect(Collectors.toMap(fg -> fg.getId().toString(), FriendGroup::isVisible));
        List<FriendGroupDetails> groups = Arrays.stream(friendCache.getClientGroups(nodeName))
                .filter(fg -> visible.get(fg.getId()) || privileged)
                .collect(Collectors.toList());

        return new FriendInfo(nodeName, groups);
    }

    @PutMapping
    @Admin
    @Transactional
    public List<FriendInfo> put(@Valid @RequestBody FriendDescription[] friendDescriptions) {
        log.info("PUT /people/friends");

        List<FriendInfo> result = new ArrayList<>();

        Map<UUID, FriendGroup> groups = new HashMap<>();
        for (FriendDescription friendDescription : friendDescriptions) {
            Map<UUID, Friend> targetGroups = new HashMap<>();
            if (friendDescription.getGroups() != null) {
                friendDescription.getGroups().forEach(id -> targetGroups.put(id, new Friend()));
            }

            List<Friend> friends = new ArrayList<>(friendRepository.findAllByNodeIdAndName(
                    requestContext.nodeId(), friendDescription.getNodeName()));
            for (Friend friend : friends) {
                if (targetGroups.containsKey(friend.getFriendGroup().getId())) {
                    targetGroups.put(friend.getFriendGroup().getId(), friend);
                } else {
                    friendRepository.delete(friend);
                }
            }

            FriendInfo friendInfo = null;
            for (var target : targetGroups.entrySet()) {
                Friend friend = target.getValue();
                if (friend.getId() == null) {
                    friend = new Friend();
                    friend.setId(UUID.randomUUID());
                    friend.setNodeName(friendDescription.getNodeName());
                    FriendGroup group = groups.computeIfAbsent(
                            target.getKey(),
                            id -> friendGroupRepository.findByNodeIdAndId(requestContext.nodeId(), id)
                                .orElseThrow(() -> new ObjectNotFoundFailure("friend-group.not-found"))
                    );
                    friend.setFriendGroup(group);
                    friend = friendRepository.save(friend);
                }
                if (friendInfo == null) {
                    friendInfo = new FriendInfo();
                    friendInfo.setNodeName(friend.getNodeName());
                    friendInfo.setGroups(new ArrayList<>());
                    result.add(friendInfo);
                }
                friendInfo.getGroups().add(new FriendGroupDetails(friend));
            }

            requestContext.invalidateFriendCache(FriendCachePart.CLIENT_GROUPS, friendDescription.getNodeName());
            requestContext.send(new FriendshipUpdatedLiberin(friendDescription.getNodeName()));
            requestContext.send(new FeaturesUpdatedLiberin(friendDescription.getNodeName()));
        }

        return result;
    }

}
