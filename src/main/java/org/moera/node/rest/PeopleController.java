package org.moera.node.rest;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.moera.node.data.SubscriberRepository;
import org.moera.node.data.SubscriptionRepository;
import org.moera.node.data.SubscriptionType;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.model.PeopleGeneralInfo;
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
    private SubscriptionRepository subscriptionRepository;

    @GetMapping
    @Transactional
    public PeopleGeneralInfo get() {
        log.info("GET /people");

        int subscribersTotal = subscriberRepository.countAllByType(requestContext.nodeId(), SubscriptionType.FEED);
        int subscriptionsTotal = subscriptionRepository.countByType(requestContext.nodeId(), SubscriptionType.FEED);

        return new PeopleGeneralInfo(subscribersTotal, subscriptionsTotal, requestContext.getOptions());
    }

}
