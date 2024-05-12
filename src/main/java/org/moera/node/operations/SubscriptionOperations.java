package org.moera.node.operations;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import javax.inject.Inject;

import org.moera.node.data.Contact;
import org.moera.node.data.Feed;
import org.moera.node.data.Subscription;
import org.moera.node.data.SubscriptionReason;
import org.moera.node.data.SubscriptionType;
import org.moera.node.data.UserSubscription;
import org.moera.node.data.UserSubscriptionRepository;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.model.SubscriptionAddedLiberin;
import org.moera.node.model.OperationFailure;
import org.moera.node.rest.task.RemoteFeedFetchJob;
import org.moera.node.task.Jobs;
import org.moera.node.util.Transaction;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class SubscriptionOperations {

    @Inject
    private UniversalContext universalContext;

    @Inject
    private UserSubscriptionRepository userSubscriptionRepository;

    @Inject
    private ContactOperations contactOperations;

    @Inject
    private Transaction tx;

    @Inject
    private Jobs jobs;

    public UserSubscription subscribe(Consumer<UserSubscription> toUserSubscription) {
        UserSubscription subscription;
        try {
            subscription = tx.executeWrite(() -> {
                UserSubscription userSubscription = new UserSubscription();
                userSubscription.setId(UUID.randomUUID());
                userSubscription.setNodeId(universalContext.nodeId());
                if (toUserSubscription != null) {
                    toUserSubscription.accept(userSubscription);
                }
                userSubscription = userSubscriptionRepository.save(userSubscription);

                Contact contact;
                if (userSubscription.getSubscriptionType() == SubscriptionType.FEED) {
                    contactOperations.updateCloseness(userSubscription.getRemoteNodeName(), 800);
                    contactOperations.updateFeedSubscriptionCount(userSubscription.getRemoteNodeName(), 1);
                    contact = contactOperations.updateViewPrincipal(userSubscription);
                } else {
                    contact = contactOperations.updateCloseness(userSubscription.getRemoteNodeName(), 1);
                }
                contact.fill(userSubscription);

                return userSubscription;
            });
        } catch (DataIntegrityViolationException e) {
            throw new OperationFailure("subscription.already-exists");
        }

        universalContext.subscriptionsUpdated();
        if (subscription.getSubscriptionType() == SubscriptionType.FEED) {
            universalContext.invalidateSubscribedCache(subscription.getRemoteNodeName());
        }
        universalContext.send(new SubscriptionAddedLiberin(subscription));

        if (subscription.getSubscriptionType() == SubscriptionType.FEED) {
            jobs.run(
                    RemoteFeedFetchJob.class,
                    new RemoteFeedFetchJob.Parameters(
                            subscription.getFeedName(),
                            subscription.getRemoteNodeName(),
                            subscription.getRemoteFeedName()),
                    universalContext.nodeId());
        }

        return subscription;
    }

    public void autoSubscribe() {
        String remoteNodeName = universalContext.getOptions().getString("subscription.auto.node");
        if (ObjectUtils.isEmpty(remoteNodeName)) {
            return;
        }
        int count = userSubscriptionRepository.countByTypeAndNodeAndFeedName(
                universalContext.nodeId(), SubscriptionType.FEED, remoteNodeName, Feed.TIMELINE);
        if (count > 0) {
            return;
        }
        subscribe(subscription -> {
            subscription.setSubscriptionType(SubscriptionType.FEED);
            subscription.setFeedName(Feed.NEWS);
            subscription.setRemoteNodeName(remoteNodeName);
            subscription.setRemoteFeedName(Feed.TIMELINE);
            subscription.setReason(SubscriptionReason.AUTO);
        });
    }

    public void subscribeToPostingComments(String nodeName, String entryId, SubscriptionReason reason) {
        int count = userSubscriptionRepository.countByTypeAndNodeAndEntryId(
                universalContext.nodeId(), SubscriptionType.POSTING_COMMENTS, nodeName, entryId);
        if (count > 0) {
            return;
        }

        UserSubscription subscription = new UserSubscription();
        subscription.setId(UUID.randomUUID());
        subscription.setNodeId(universalContext.nodeId());
        subscription.setSubscriptionType(SubscriptionType.POSTING_COMMENTS);
        subscription.setRemoteNodeName(nodeName);
        subscription.setRemoteEntryId(entryId);
        subscription.setReason(reason);
        subscription = userSubscriptionRepository.save(subscription);

        contactOperations.updateCloseness(nodeName, 1).fill(subscription);

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
                contactOperations.updateFeedSubscriptionCount(subscription.getRemoteNodeName(), -1);
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
            case USER_LIST:
                userSubscriptions = userSubscriptionRepository.findAllByTypeAndNodeAndFeedName(
                        universalContext.nodeId(), subscription.getSubscriptionType(), subscription.getRemoteNodeName(),
                        subscription.getRemoteFeedName());
                break;
        }
        if (userSubscriptions != null) {
            userSubscriptions.forEach(us -> userSubscriptionRepository.delete(us));
        }
    }

}
