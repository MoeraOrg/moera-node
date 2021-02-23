package org.moera.node.rest.task;

import java.util.UUID;
import javax.inject.Inject;

import org.moera.node.api.NodeApiUnknownNameException;
import org.moera.node.data.Subscription;
import org.moera.node.data.SubscriptionRepository;
import org.moera.node.data.SubscriptionType;
import org.moera.node.model.SubscriberDescriptionQ;
import org.moera.node.model.SubscriberInfo;
import org.moera.node.model.event.SubscriptionAddedEvent;
import org.moera.node.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteProfileSubscribeTask extends Task {

    private static Logger log = LoggerFactory.getLogger(RemoteProfileSubscribeTask.class);

    private String targetNodeName;
    private String targetFullName;

    @Inject
    private SubscriptionRepository subscriptionRepository;

    public RemoteProfileSubscribeTask(String targetNodeName) {
        this.targetNodeName = targetNodeName;
    }

    @Override
    public void run() {
        initLoggingDomain();
        try {
            nodeApi.setNodeId(nodeId);
            boolean subscribed = subscriptionRepository.countByTypeAndRemoteNode(nodeId, SubscriptionType.PROFILE,
                    targetNodeName) > 0;
            if (subscribed) {
                return;
            }
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
            subscription = subscriptionRepository.save(subscription);
            send(new SubscriptionAddedEvent(subscription));
            success();
        } catch (Exception e) {
            error(e);
        }
    }

    private void success() {
        log.info("Succeeded to subscribe to profile of node {}", targetNodeName);
    }

    private void error(Throwable e) {
        if (e instanceof NodeApiUnknownNameException) {
            log.error("Cannot find a node {}", targetNodeName);
        } else {
            log.error("Error subscribing to profile of node {}: {}", targetNodeName, e.getMessage());
        }
    }

}
