package org.moera.node.rest;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.data.FriendGroup;
import org.moera.node.data.FriendGroupRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.model.FriendGroupDescription;
import org.moera.node.model.FriendGroupInfo;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    @GetMapping
    @Transactional
    public List<FriendGroupInfo> getAll() {
        log.info("GET /people/friends/groups");

        return friendGroupRepository.findAllByNodeId(requestContext.nodeId()).stream()
                .filter(fr -> requestContext.isAdmin() || fr.isVisible())
                .map(FriendGroupInfo::new)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @Transactional
    public FriendGroupInfo get(@PathVariable UUID id) {
        log.info("GET /people/friends/groups/{id} (id = {})", LogUtil.format(id));

        FriendGroup friendGroup = friendGroupRepository.findByNodeIdAndId(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("friend-group.not-found"));
        if (!requestContext.isAdmin() && !friendGroup.isVisible()) {
            throw new ObjectNotFoundFailure("friend-group.not-found");
        }

        return new FriendGroupInfo(friendGroup);
    }

    @PostMapping
    @Admin
    @Transactional
    public FriendGroupInfo post(@Valid @RequestBody FriendGroupDescription friendGroupDescription) {
        log.info("POST /people/friends/groups (title = {}, visible = {})",
                LogUtil.format(friendGroupDescription.getTitle()), LogUtil.format(friendGroupDescription.getVisible()));

        FriendGroup friendGroup = new FriendGroup();
        friendGroup.setId(UUID.randomUUID());
        friendGroupDescription.toFriendGroup(friendGroup);
        friendGroup = friendGroupRepository.save(friendGroup);

        return new FriendGroupInfo(friendGroup);
    }

    @PutMapping("/{id}")
    @Admin
    @Transactional
    public FriendGroupInfo put(@PathVariable UUID id,
                               @Valid @RequestBody FriendGroupDescription friendGroupDescription) {
        log.info("PUT /people/friends/groups/{id} (id = {}, title = {}, visible = {})",
                LogUtil.format(id), LogUtil.format(friendGroupDescription.getTitle()),
                LogUtil.format(friendGroupDescription.getVisible()));

        FriendGroup friendGroup = friendGroupRepository.findByNodeIdAndId(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("friend-group.not-found"));
        friendGroupDescription.toFriendGroup(friendGroup);

        return new FriendGroupInfo(friendGroup);
    }

    @DeleteMapping("/{id}")
    @Admin
    @Transactional
    public Result delete(@PathVariable UUID id) {
        log.info("DELETE /people/friends/groups/{id} (id = {})", LogUtil.format(id));

        FriendGroup friendGroup = friendGroupRepository.findByNodeIdAndId(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("friend-group.not-found"));
        friendGroupRepository.delete(friendGroup);

        return Result.OK;
    }

}
