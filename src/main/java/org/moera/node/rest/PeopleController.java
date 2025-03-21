package org.moera.node.rest;

import java.util.Map;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.moera.lib.node.types.PeopleGeneralInfo;
import org.moera.lib.node.types.SubscriptionType;
import org.moera.node.data.BlockedByUserRepository;
import org.moera.node.data.BlockedUserRepository;
import org.moera.node.data.FriendOfRepository;
import org.moera.node.data.FriendRepository;
import org.moera.node.data.SubscriberRepository;
import org.moera.node.data.UserSubscriptionRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.model.PeopleGeneralInfoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/people")
@NoCache
public class PeopleController {

    private static final Logger log = LoggerFactory.getLogger(PeopleController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private SubscriberRepository subscriberRepository;

    @Inject
    private UserSubscriptionRepository userSubscriptionRepository;

    @Inject
    private FriendRepository friendRepository;

    @Inject
    private FriendOfRepository friendOfRepository;

    @Inject
    private BlockedUserRepository blockedUserRepository;

    @Inject
    private BlockedByUserRepository blockedByUserRepository;

    @GetMapping
    @Transactional
    public PeopleGeneralInfo get() {
        log.info("GET /people");

        int subscribersTotal = subscriberRepository.countAllByType(requestContext.nodeId(), SubscriptionType.FEED);
        int subscriptionsTotal = userSubscriptionRepository.countByType(requestContext.nodeId(), SubscriptionType.FEED);
        Map<String, Integer> friendsTotal = friendRepository.countGroupsByNodeId(requestContext.nodeId()).stream()
                .collect(Collectors.toMap(fg -> fg.getId().toString(), fg -> (int) fg.getTotal()));
        int friendOfsTotal = friendOfRepository.countByNodeId(requestContext.nodeId());
        int blockedTotal = blockedUserRepository.countByNodeId(requestContext.nodeId());
        int blockedByTotal = blockedByUserRepository.countByNodeId(requestContext.nodeId());

        return PeopleGeneralInfoUtil.build(
            subscribersTotal, subscriptionsTotal, friendsTotal, friendOfsTotal, blockedTotal, blockedByTotal,
            requestContext.getOptions(), requestContext
        );
    }

}
