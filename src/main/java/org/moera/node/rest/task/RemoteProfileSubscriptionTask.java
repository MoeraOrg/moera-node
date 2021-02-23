package org.moera.node.rest.task;

import java.util.UUID;
import javax.inject.Inject;

import org.moera.node.api.NodeApiException;
import org.moera.node.api.NodeApiUnknownNameException;
import org.moera.node.data.SubscriberRepository;
import org.moera.node.data.Subscription;
import org.moera.node.data.SubscriptionRepository;
import org.moera.node.data.SubscriptionType;
import org.moera.node.model.SubscriberDescriptionQ;
import org.moera.node.model.SubscriberInfo;
import org.moera.node.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteProfileSubscriptionTask extends Task {

    private static Logger log = LoggerFactory.getLogger(RemoteProfileSubscriptionTask.class);

    private String targetNodeName;
    private String targetFullName;

    @Inject
    private SubscriptionRepository subscriptionRepository;

    @Inject
    private SubscriberRepository subscriberRepository;

    public RemoteProfileSubscriptionTask(String targetNodeName) {
        this.targetNodeName = targetNodeName;
    }

    @Override
    public void run() {
        initLoggingDomain();
        boolean subscribe = shouldSubscribe();
        try {
            nodeApi.setNodeId(nodeId);
            Subscription subscription = subscriptionRepository.findByTypeAndRemoteNode(nodeId, SubscriptionType.PROFILE,
                    targetNodeName).orElse(null);
            if (subscribe) {
                if (subscription != null) {
                    return;
                }
                subscribe();
            } else {
                if (subscription == null) {
                    return;
                }
                unsubscribe(subscription);
            }
            success(subscribe);
        } catch (Exception e) {
            error(subscribe, e);
        }
    }

    private boolean shouldSubscribe() {
        return subscriberRepository.countByType(nodeId, targetNodeName, SubscriptionType.FEED) > 0
                || subscriptionRepository.countByTypeAndRemoteNode(nodeId, SubscriptionType.FEED, targetNodeName) > 0;
    }

    private void subscribe() throws NodeApiException {
        targetFullName = nodeApi.whoAmI(targetNodeName).getFullName();
        SubscriberDescriptionQ description = new SubscriberDescriptionQ(SubscriptionType.PROFILE,
                null, null, fullName);
        SubscriberInfo subscriberInfo =
                nodeApi.postSubscriber(targetNodeName, generateCarte(targetNodeName), description);
        Subscription subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setNodeId(nodeId);
        subscription.setSubscriptionType(SubscriptionType.PROFILE);
        subscription.setRemoteSubscriberId(subscriberInfo.getId());
        subscription.setRemoteNodeName(targetNodeName);
        subscription.setRemoteFullName(targetFullName);
        subscriptionRepository.save(subscription);
    }

    private void unsubscribe(Subscription subscription) throws NodeApiException {
        subscriptionRepository.delete(subscription);
        nodeApi.deleteSubscriber(targetNodeName, generateCarte(targetNodeName), subscription.getRemoteSubscriberId());
    }

    private void success(boolean subscribe) {
        if (subscribe) {
            log.info("Succeeded to subscribe to profile of node {}", targetNodeName);
        } else {
            log.info("Succeeded to unsubscribe from profile of node {}", targetNodeName);
        }
    }

    private void error(boolean subscribe, Throwable e) {
        if (e instanceof NodeApiUnknownNameException) {
            log.error("Cannot find a node {}", targetNodeName);
        } else {
            if (subscribe) {
                log.error("Error subscribing to profile of node {}: {}", targetNodeName, e.getMessage());
            } else {
                log.error("Error unsubscribing from profile of node {}: {}", targetNodeName, e.getMessage());
            }
        }
    }

}
