package org.moera.node.rest.task;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.inject.Inject;

import org.moera.node.api.node.NodeApiException;
import org.moera.node.auth.principal.Principal;
import org.moera.node.data.Subscription;
import org.moera.node.data.SubscriptionRepository;
import org.moera.node.data.SubscriptionType;
import org.moera.node.model.SubscriberOverride;
import org.moera.node.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AllRemoteSubscribersUpdateTask extends Task {

    private static final Logger log = LoggerFactory.getLogger(AllRemoteSubscribersUpdateTask.class);

    private final UUID nodeId;
    private final Principal viewPrincipal;

    @Inject
    private SubscriptionRepository subscriptionRepository;

    public AllRemoteSubscribersUpdateTask(UUID nodeId, Principal viewPrincipal) {
        this.nodeId = nodeId;
        this.viewPrincipal = viewPrincipal;
    }

    @Override
    protected void execute() {
        List<Subscription> subscriptions = subscriptionRepository.findAllByType(nodeId, SubscriptionType.FEED);
        SubscriberOverride override = new SubscriberOverride();
        override.setOperations(Collections.singletonMap("view", viewPrincipal));
        for (Subscription subscription : subscriptions) {
            log.info("Updating subscriber info at node {}", subscription.getRemoteNodeName());
            try {
                nodeApi.putSubscriber(subscription.getRemoteNodeName(), generateCarte(subscription.getRemoteNodeName()),
                        subscription.getRemoteSubscriberId(), override);
            } catch (NodeApiException e) {
                log.warn("Error updating subscriber info at node {}: {}",
                        subscription.getRemoteNodeName(), e.getMessage());
            }
        }
    }

}
