package org.moera.node.rest;

import java.util.UUID;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.data.Feed;
import org.moera.node.data.Subscription;
import org.moera.node.data.SubscriptionRepository;
import org.moera.node.data.SubscriptionType;
import org.moera.node.global.ApiController;
import org.moera.node.global.RequestContext;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.Result;
import org.moera.node.model.SubscriptionDescription;
import org.moera.node.model.SubscriptionInfo;
import org.moera.node.model.ValidationFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@ApiController
@RequestMapping("/moera/api/subscriptions")
public class SubscriptionController {

    private static Logger log = LoggerFactory.getLogger(SubscriptionController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private SubscriptionRepository subscriptionRepository;

    @PostMapping
    @Admin
    @Transactional
    public SubscriptionInfo post(@Valid @RequestBody SubscriptionDescription subscriptionDescription) {
        log.info("POST /subscriptions (type = {}, feedName = {}, remoteSubscriberId = {}, remoteNodeName = {})",
                LogUtil.format(SubscriptionType.toValue(subscriptionDescription.getType())),
                LogUtil.format(subscriptionDescription.getFeedName()),
                LogUtil.format(subscriptionDescription.getRemoteSubscriberId()),
                LogUtil.format(subscriptionDescription.getRemoteNodeName()));

        if (subscriptionDescription.getType() == null) {
            throw new ValidationFailure("subscriptionDescription.type.blank");
        }
        if (!Feed.isStandard(subscriptionDescription.getFeedName())) {
            throw new ValidationFailure("subscriptionDescription.feedName.not-found");
        }
        boolean exists = subscriptionRepository.countBySubscriber(
                requestContext.nodeId(),
                subscriptionDescription.getType(),
                subscriptionDescription.getRemoteNodeName(),
                subscriptionDescription.getRemoteSubscriberId()) > 0;
        if (exists) {
            throw new OperationFailure("subscription.already-exists");
        }

        Subscription subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setNodeId(requestContext.nodeId());
        subscriptionDescription.toSubscription(subscription);
        subscription = subscriptionRepository.save(subscription);

        return new SubscriptionInfo(subscription);
    }

    @DeleteMapping
    @Admin
    @Transactional
    public Result delete(
            @RequestParam(defaultValue = "feed") SubscriptionType type,
            @RequestParam("nodeName") String remoteNodeName,
            @RequestParam("subscriberId") String remoteSubscriberId) {

        log.info("DELETE /subscriptions (type = {}, remoteSubscriberId = {}, remoteNodeName = {})",
                LogUtil.format(SubscriptionType.toValue(type)),
                LogUtil.format(remoteSubscriberId),
                LogUtil.format(remoteNodeName));

        Subscription subscription = subscriptionRepository.findBySubscriber(requestContext.nodeId(), type,
                remoteNodeName, remoteSubscriberId).orElse(null);
        if (subscription == null) {
            throw new ObjectNotFoundFailure("subscription.not-found");
        }

        subscriptionRepository.delete(subscription);

        return Result.OK;
    }

}
