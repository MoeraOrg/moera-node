package org.moera.node.rest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.moera.lib.node.types.ContactInfo;
import org.moera.lib.node.types.FriendGroupDetails;
import org.moera.lib.node.types.FriendOfInfo;
import org.moera.lib.node.types.Scope;
import org.moera.lib.util.LogUtil;
import org.moera.node.auth.AuthenticationException;
import org.moera.node.data.FriendOf;
import org.moera.node.data.FriendOfRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.model.ContactInfoUtil;
import org.moera.node.model.FriendGroupDetailsUtil;
import org.moera.node.model.FriendOfInfoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/people/friend-ofs")
@NoCache
public class FriendOfController {

    private static final Logger log = LoggerFactory.getLogger(FriendOfController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private FriendOfRepository friendOfRepository;

    @GetMapping
    @Transactional
    public List<FriendOfInfo> getAll() {
        log.info("GET /people/friend-ofs");

        if (!requestContext.isPrincipal(FriendOf.getViewAllE(requestContext.getOptions()), Scope.VIEW_PEOPLE)) {
            throw new AuthenticationException();
        }

        List<FriendOf> friendOfs = friendOfRepository.findAllByNodeId(requestContext.nodeId());

        List<FriendOfInfo> friendOfInfos = new ArrayList<>();
        List<FriendGroupDetails> groups = null;
        String prevNodeName = null;
        for (FriendOf friendOf : friendOfs) {
            if (prevNodeName != null && !prevNodeName.equals(friendOf.getRemoteNodeName())) {
                groups = null;
            }
            if (groups == null) {
                FriendOfInfo info = FriendOfInfoUtil.build(friendOf, requestContext.getOptions(), requestContext);
                groups = new ArrayList<>();
                info.setGroups(groups);
                friendOfInfos.add(info);
                prevNodeName = friendOf.getRemoteNodeName();
            }
            groups.add(FriendGroupDetailsUtil.build(friendOf));
        }

        return friendOfInfos.stream().filter(fi -> !fi.getGroups().isEmpty()).collect(Collectors.toList());
    }

    @GetMapping("/{name}")
    @Transactional
    public FriendOfInfo get(@PathVariable("name") String nodeName) {
        log.info("GET /people/friend-ofs/{name} (name = {})", LogUtil.format(nodeName));

        if (!requestContext.isPrincipal(FriendOf.getViewAllE(requestContext.getOptions()), Scope.VIEW_PEOPLE)
                && !requestContext.isClient(nodeName, Scope.VIEW_PEOPLE)) {
            throw new AuthenticationException();
        }

        List<FriendOf> friendOfs = friendOfRepository.findByNodeIdAndRemoteNode(requestContext.nodeId(), nodeName);
        if (friendOfs.isEmpty()) {
            return FriendOfInfoUtil.build(nodeName, null, Collections.emptyList());
        }

        List<FriendGroupDetails> groups = friendOfs.stream()
                .map(FriendGroupDetailsUtil::build)
                .collect(Collectors.toList());

        ContactInfo contactInfo = ContactInfoUtil.build(
            friendOfs.get(0).getContact(), requestContext.getOptions(), requestContext
        );
        return FriendOfInfoUtil.build(nodeName, contactInfo, groups);
    }

}
