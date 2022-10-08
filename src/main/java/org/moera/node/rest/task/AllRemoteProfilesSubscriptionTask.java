package org.moera.node.rest.task;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.inject.Inject;

import org.moera.node.api.NodeApiUnknownNameException;
import org.moera.node.data.ContactRepository;
import org.moera.node.data.DomainUpgradeRepository;
import org.moera.node.data.Subscriber;
import org.moera.node.data.SubscriberRepository;
import org.moera.node.data.Subscription;
import org.moera.node.data.SubscriptionRepository;
import org.moera.node.data.SubscriptionType;
import org.moera.node.data.UpgradeType;
import org.moera.node.model.SubscriberDescriptionQ;
import org.moera.node.model.SubscriberInfo;
import org.moera.node.model.WhoAmI;
import org.moera.node.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AllRemoteProfilesSubscriptionTask extends Task {

    private static final Logger log = LoggerFactory.getLogger(AllRemoteProfilesSubscriptionTask.class);

    @Inject
    private SubscriptionRepository subscriptionRepository;

    @Inject
    private SubscriberRepository subscriberRepository;

    @Inject
    private ContactRepository contactRepository;

    @Inject
    private DomainUpgradeRepository domainUpgradeRepository;

    public AllRemoteProfilesSubscriptionTask() {
    }

    @Override
    protected void execute() {
        Set<String> targetNodeNames = getTargetNodeNames();
        for (String targetNodeName : targetNodeNames) {
            Duration delay = Duration.ofSeconds(30);
            for (int i = 0; i < 5; i++) {
                try {
                    boolean subscribed = subscriptionRepository.countByTypeAndRemoteNode(
                            nodeId, SubscriptionType.PROFILE, targetNodeName) > 0;
                    if (subscribed) {
                        break;
                    }
                    subscribe(targetNodeName);
                    success(targetNodeName);
                    break;
                } catch (Throwable e) {
                    error(targetNodeName, e);
                }
                try {
                    Thread.sleep(delay.toMillis());
                } catch (InterruptedException e) {
                }
                delay = delay.multipliedBy(2);
            }
        }
        try {
            inTransaction(() -> {
                domainUpgradeRepository.deleteByTypeAndNode(UpgradeType.PROFILE_SUBSCRIBE, nodeId);
                return null;
            });
        } catch (Throwable t) {
            log.error("Error deleting domain upgrade record: {}", t.getMessage());
        }
    }

    private Set<String> getTargetNodeNames() {
        Set<String> nodeNames = new HashSet<>();
        subscriberRepository.findAllByType(nodeId, SubscriptionType.FEED).stream()
                .map(Subscriber::getRemoteNodeName)
                .forEach(nodeNames::add);
        subscriptionRepository.findAllByType(nodeId, SubscriptionType.FEED).stream()
                .map(Subscription::getRemoteNodeName)
                .forEach(nodeNames::add);
        return nodeNames;
    }

    private void subscribe(String targetNodeName) throws Throwable {
        WhoAmI target = nodeApi.whoAmI(targetNodeName);
        String targetFullName = target.getFullName();
        String targetGender = target.getGender();
        inTransaction(() -> {
            subscriberRepository.updateRemoteFullNameAndGender(nodeId, targetNodeName, targetFullName, targetGender);
            subscriptionRepository.updateRemoteFullNameAndGender(nodeId, targetNodeName, targetFullName, targetGender);
            contactRepository.updateRemoteFullNameAndGender(nodeId, targetNodeName, targetFullName, targetGender);
            return null;
        });

        SubscriberDescriptionQ description = new SubscriberDescriptionQ(SubscriptionType.PROFILE,
                null, null, fullName(), gender(), getAvatar());
        SubscriberInfo subscriberInfo =
                nodeApi.postSubscriber(targetNodeName, generateCarte(targetNodeName), description);
        Subscription subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setNodeId(nodeId);
        subscription.setSubscriptionType(SubscriptionType.PROFILE);
        subscription.setRemoteSubscriberId(subscriberInfo.getId());
        subscription.setRemoteNodeName(targetNodeName);
        subscription.setRemoteFullName(targetFullName);
        subscription.setRemoteGender(targetGender);
        subscriptionRepository.save(subscription);
    }

    private void success(String targetNodeName) {
        log.info("Succeeded to subscribe to profile of node {}", targetNodeName);
    }

    private void error(String targetNodeName, Throwable e) {
        if (e instanceof NodeApiUnknownNameException) {
            log.error("Cannot find a node {}", targetNodeName);
        } else {
            log.error("Error subscribing to profile of node {}: {}", targetNodeName, e.getMessage());
        }
    }

}
