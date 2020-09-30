package org.moera.node.rest.task;

import java.util.UUID;
import javax.inject.Inject;

import org.moera.node.api.NodeApiUnknownNameException;
import org.moera.node.data.Subscription;
import org.moera.node.data.SubscriptionReason;
import org.moera.node.data.SubscriptionRepository;
import org.moera.node.data.SubscriptionType;
import org.moera.node.model.SubscriberDescriptionQ;
import org.moera.node.model.SubscriberInfo;
import org.moera.node.model.event.SubscriptionAddedEvent;
import org.moera.node.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemotePostingCommentsSubscribeTask extends Task {

    private static Logger log = LoggerFactory.getLogger(RemotePostingCommentsSubscribeTask.class);

    private String targetNodeName;
    private String postingId;
    private SubscriptionReason reason;

    @Inject
    private SubscriptionRepository subscriptionRepository;

    public RemotePostingCommentsSubscribeTask(String targetNodeName, String postingId, SubscriptionReason reason) {
        this.targetNodeName = targetNodeName;
        this.postingId = postingId;
        this.reason = reason;
    }

    @Override
    public void run() {
        try {
            nodeApi.setNodeId(nodeId);
            boolean subscribed = !subscriptionRepository.findAllByTypeAndNodeAndEntryId(nodeId,
                    SubscriptionType.POSTING_COMMENTS, targetNodeName, postingId).isEmpty();
            if (subscribed) {
                return;
            }
            SubscriberDescriptionQ description = new SubscriberDescriptionQ(SubscriptionType.POSTING_COMMENTS,
                    null, postingId);
            SubscriberInfo subscriberInfo = nodeApi.postSubscriber(targetNodeName, generateCarte(), description);
            Subscription subscription = new Subscription();
            subscription.setId(UUID.randomUUID());
            subscription.setNodeId(nodeId);
            subscription.setSubscriptionType(SubscriptionType.POSTING_COMMENTS);
            subscription.setRemoteSubscriberId(subscriberInfo.getId());
            subscription.setRemoteNodeName(targetNodeName);
            subscription.setRemoteEntryId(postingId);
            subscription.setReason(reason);
            subscription = subscriptionRepository.save(subscription);
            send(new SubscriptionAddedEvent(subscription));
            success();
        } catch (Exception e) {
            error(e);
        }
    }

    private void success() {
        initLoggingDomain();
        log.info("Succeeded to subscribe to comments to posting {} at node {}", postingId, targetNodeName);
    }

    private void error(Throwable e) {
        initLoggingDomain();
        if (e instanceof NodeApiUnknownNameException) {
            log.error("Cannot find a node {}", targetNodeName);
        } else {
            log.error("Error subscribing to comments to posting {} at node {}: {}", postingId, targetNodeName,
                    e.getMessage());
        }
    }

}
