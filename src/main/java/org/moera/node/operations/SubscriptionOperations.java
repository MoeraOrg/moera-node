package org.moera.node.operations;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.moera.node.data.Subscription;
import org.moera.node.data.SubscriptionReason;
import org.moera.node.data.SubscriptionType;
import org.moera.node.data.UserSubscription;
import org.moera.node.data.UserSubscriptionRepository;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.model.SubscriptionAddedLiberin;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionOperations {

    @Inject
    private UniversalContext universalContext;

    @Inject
    private UserSubscriptionRepository userSubscriptionRepository;

    public void subscribeToPostingComments(String nodeName, String entryId, SubscriptionReason reason) {
        UserSubscription subscription = new UserSubscription();
        subscription.setId(UUID.randomUUID());
        subscription.setNodeId(universalContext.nodeId());
        subscription.setSubscriptionType(SubscriptionType.POSTING_COMMENTS);
        subscription.setRemoteNodeName(nodeName);
        subscription.setRemoteEntryId(entryId);
        subscription.setReason(reason);
        subscription = userSubscriptionRepository.save(subscription);
        universalContext.send(new SubscriptionAddedLiberin(subscription));
        universalContext.subscriptionsUpdated();
    }

    public void deleteParents(Subscription subscription) {
        List<UserSubscription> userSubscriptions = null;
        switch (subscription.getSubscriptionType()) {
            case FEED:
                userSubscriptions = userSubscriptionRepository.findAllByTypeAndNodeAndFeedName(
                        universalContext.nodeId(), subscription.getSubscriptionType(), subscription.getRemoteNodeName(),
                        subscription.getRemoteFeedName());
                break;
            case POSTING:
            case POSTING_COMMENTS:
                userSubscriptions = userSubscriptionRepository.findAllByTypeAndNodeAndEntryId(
                        universalContext.nodeId(), subscription.getSubscriptionType(), subscription.getRemoteNodeName(),
                        subscription.getRemoteEntryId());
                break;
            case PROFILE:
                userSubscriptions = userSubscriptionRepository.findAllByType(
                        universalContext.nodeId(), subscription.getSubscriptionType());
                break;
        }
        if (userSubscriptions != null) {
            userSubscriptions.forEach(us -> userSubscriptionRepository.delete(us));
        }
    }

}
