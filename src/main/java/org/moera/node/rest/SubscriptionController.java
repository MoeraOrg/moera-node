package org.moera.node.rest;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import com.querydsl.core.BooleanBuilder;
import org.moera.commons.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.data.Feed;
import org.moera.node.data.QSubscription;
import org.moera.node.data.Subscription;
import org.moera.node.data.SubscriptionRepository;
import org.moera.node.data.SubscriptionType;
import org.moera.node.global.ApiController;
import org.moera.node.global.Entitled;
import org.moera.node.global.RequestContext;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.RemotePosting;
import org.moera.node.model.Result;
import org.moera.node.model.SubscriptionDescription;
import org.moera.node.model.SubscriptionFilter;
import org.moera.node.model.SubscriptionInfo;
import org.moera.node.model.ValidationFailure;
import org.moera.node.model.event.SubscriptionAddedEvent;
import org.moera.node.model.event.SubscriptionDeletedEvent;
import org.moera.node.operations.ContactOperations;
import org.moera.node.rest.task.RemoteFeedFetchTask;
import org.moera.node.task.TaskAutowire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@ApiController
@RequestMapping("/moera/api/people/subscriptions")
public class SubscriptionController {

    private static Logger log = LoggerFactory.getLogger(SubscriptionController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private SubscriptionRepository subscriptionRepository;

    @Inject
    private ContactOperations contactOperations;

    @Inject
    @Qualifier("remoteTaskExecutor")
    private TaskExecutor taskExecutor;

    @Inject
    private TaskAutowire taskAutowire;

    @GetMapping
    public List<SubscriptionInfo> getAll(@RequestParam(required = false) String nodeName,
                                         @RequestParam(required = false) SubscriptionType type) {
        log.info("GET /people/subscriptions (nodeName = {}, type = {})",
                LogUtil.format(nodeName), LogUtil.format(SubscriptionType.toValue(type)));

        QSubscription subscription = QSubscription.subscription;
        BooleanBuilder where = new BooleanBuilder();
        where.and(subscription.nodeId.eq(requestContext.nodeId()));
        if (!StringUtils.isEmpty(nodeName)) {
            where.and(subscription.remoteNodeName.eq(nodeName));
        }
        if (type != null) {
            where.and(subscription.subscriptionType.eq(type));
        }

        return StreamSupport.stream(subscriptionRepository.findAll(where).spliterator(), false)
                .map(SubscriptionInfo::new)
                .collect(Collectors.toList());
    }

    @PostMapping
    @Admin
    @Entitled
    @Transactional
    public SubscriptionInfo post(@Valid @RequestBody SubscriptionDescription subscriptionDescription) {
        log.info("POST /people/subscriptions (type = {}, feedName = {}, remoteSubscriberId = {}, remoteNodeName = {},"
                        + " remotePostingId = {})",
                LogUtil.format(SubscriptionType.toValue(subscriptionDescription.getType())),
                LogUtil.format(subscriptionDescription.getFeedName()),
                LogUtil.format(subscriptionDescription.getRemoteSubscriberId()),
                LogUtil.format(subscriptionDescription.getRemoteNodeName()),
                LogUtil.format(subscriptionDescription.getRemotePostingId()));

        if (subscriptionDescription.getType() == null) {
            throw new ValidationFailure("subscriptionDescription.type.blank");
        }
        if (subscriptionDescription.getReason() == null) {
            throw new ValidationFailure("subscriptionDescription.reason.blank");
        }
        if (subscriptionDescription.getType() == SubscriptionType.FEED) {
            if (StringUtils.isEmpty(subscriptionDescription.getFeedName())) {
                throw new ValidationFailure("subscriptionDescription.feedName.blank");
            }
            if (!Feed.isStandard(subscriptionDescription.getFeedName())) {
                throw new ValidationFailure("subscriptionDescription.feedName.not-found");
            }
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
        if (subscription.getSubscriptionType() == SubscriptionType.FEED) {
            contactOperations.createOrUpdateCloseness(subscription.getRemoteNodeName(),
                    subscription.getRemoteFullName(), 1);
        } else {
            contactOperations.updateCloseness(subscription.getRemoteNodeName(), subscription.getRemoteFullName(), 1);
        }
        requestContext.send(new SubscriptionAddedEvent(subscription));

        if (subscription.getSubscriptionType() == SubscriptionType.FEED) {
            var task = new RemoteFeedFetchTask(subscription.getFeedName(), subscription.getRemoteNodeName(),
                    subscription.getRemoteFeedName());
            taskAutowire.autowire(task);
            taskExecutor.execute(task);
        }

        return new SubscriptionInfo(subscription);
    }

    @DeleteMapping
    @Admin
    @Transactional
    public Result delete(
            @RequestParam("nodeName") String remoteNodeName,
            @RequestParam("subscriberId") String remoteSubscriberId) {

        log.info("DELETE /people/subscriptions (remoteSubscriberId = {}, remoteNodeName = {})",
                LogUtil.format(remoteSubscriberId),
                LogUtil.format(remoteNodeName));

        Subscription subscription = subscriptionRepository.findBySubscriber(requestContext.nodeId(), remoteNodeName,
                remoteSubscriberId)
                .orElseThrow(() -> new ObjectNotFoundFailure("subscription.not-found"));
        int totalSubscriptions = subscriptionRepository.countByTypeAndRemoteNode(requestContext.nodeId(),
                SubscriptionType.FEED, remoteNodeName);
        subscriptionRepository.delete(subscription);
        if (totalSubscriptions == 1) {
            contactOperations.delete(remoteNodeName);
        }
        requestContext.send(new SubscriptionDeletedEvent(subscription));

        return Result.OK;
    }

    @PostMapping("/search")
    @Admin
    public List<SubscriptionInfo> search(@Valid @RequestBody SubscriptionFilter filter) {
        log.info("GET /people/subscriptions/search");

        if (filter.getPostings() == null || filter.getPostings().isEmpty()) {
            return Collections.emptyList();
        }

        List<String> remotePostingIds = filter.getPostings().stream()
                .map(RemotePosting::getPostingId)
                .collect(Collectors.toList());
        List<Subscription> subscriptions = subscriptionRepository.findAllByRemotePostingIds(
                requestContext.nodeId(), remotePostingIds);

        return subscriptions.stream()
                .filter(r -> filter.getPostings().contains(r.getRemotePosting()))
                .map(SubscriptionInfo::new)
                .collect(Collectors.toList());
    }

}
