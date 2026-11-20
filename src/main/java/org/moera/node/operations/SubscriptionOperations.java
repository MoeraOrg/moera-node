package org.moera.node.operations;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import jakarta.inject.Inject;

import org.hibernate.exception.ConstraintViolationException;
import org.moera.lib.node.types.SubscriptionReason;
import org.moera.lib.node.types.StoryType;
import org.moera.lib.node.types.SubscriptionType;
import org.moera.node.data.Contact;
import org.moera.node.data.FavorType;
import org.moera.node.data.Feed;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.data.Subscription;
import org.moera.node.data.SubscriptionRepository;
import org.moera.node.data.UserSubscription;
import org.moera.node.data.UserSubscriptionRepository;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.model.SubscriptionAddedLiberin;
import org.moera.node.liberin.model.StoryDeletedLiberin;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.OperationFailure;
import org.moera.node.rest.task.RemoteFeedFetchJob;
import org.moera.node.task.Jobs;
import org.moera.node.util.Transaction;
import org.moera.node.util.Util;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class SubscriptionOperations {

    @Inject
    private UniversalContext universalContext;

    @Inject
    private SubscriptionRepository subscriptionRepository;

    @Inject
    private UserSubscriptionRepository userSubscriptionRepository;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    @Lazy
    private PostingOperations postingOperations;

    @Inject
    private StoryRepository storyRepository;

    @Inject
    private ContactOperations contactOperations;

    @Inject
    private FavorOperations favorOperations;

    @Inject
    private Transaction tx;

    @Inject
    private Jobs jobs;

    public UserSubscription subscribe(Consumer<UserSubscription> toUserSubscription) {
        UserSubscription subscription = tx.executeWrite(() -> {
            UserSubscription draft = new UserSubscription();
            draft.setId(UUID.randomUUID());
            draft.setNodeId(universalContext.nodeId());
            if (toUserSubscription != null) {
                toUserSubscription.accept(draft);
            }
            if (draft.getSubscriptionType() == SubscriptionType.FEED) {
                return createFeedSubscription(draft);
            }
            UserSubscription saved = userSubscriptionRepository.save(draft);
            contactOperations.find(saved.getRemoteNodeName()).fill(saved);
            return saved;
        });

        universalContext.subscriptionsUpdated();
        if (subscription.getSubscriptionType() == SubscriptionType.FEED) {
            universalContext.invalidateSubscribedCache(subscription.getRemoteNodeName());
        }
        universalContext.send(new SubscriptionAddedLiberin(subscription));

        if (
            subscription.getSubscriptionType() == SubscriptionType.FEED
            && !subscription.getRemoteNodeName()
                    .equals(universalContext.getOptions().getString("subscription.auto.node"))
        ) {
            jobs.run(
                RemoteFeedFetchJob.class,
                new RemoteFeedFetchJob.Parameters(
                    subscription.getFeedName(),
                    subscription.getRemoteNodeName(),
                    subscription.getRemoteFeedName()
                ),
                universalContext.nodeId()
            );
        }

        return subscription;
    }

    private UserSubscription createFeedSubscription(UserSubscription draft) {
        contactOperations.lockForTransaction(draft.getNodeId(), draft.getRemoteNodeName());
        draft.setRemoteEntryId(null);

        UserSubscription subscription;
        try {
            subscription = userSubscriptionRepository.saveAndFlush(draft);
        } catch (DataIntegrityViolationException e) {
            if (
                e.getCause() instanceof ConstraintViolationException violation
                && "user_subscriptions_feed_identity_idx".equals(violation.getConstraintName())
            ) {
                throw new OperationFailure("subscription.already-exists");
            }
            throw e;
        }

        // Both inserts update the same PROFILE registration through PostgreSQL triggers.
        Contact contact = contactOperations.findOrCreate(subscription.getNodeId(), subscription.getRemoteNodeName());
        contact.setFeedSubscriptionCount(contact.getFeedSubscriptionCount() + 1);
        subscription.toContactViewPrincipal(contact);
        contact.fill(subscription);
        unsetRecommendedPostings(subscription);

        return subscription;
    }

    private void unsetRecommendedPostings(UserSubscription userSubscription) {
        postingRepository.findByOwnerNameAndFeed(
            userSubscription.getNodeId(), userSubscription.getRemoteNodeName(), userSubscription.getFeedName()
        ).forEach(postingOperations::unsetRecommended);
        if (!Feed.EXPLORE.equals(userSubscription.getFeedName())) {
            postingRepository.findByOwnerNameAndFeed(
                userSubscription.getNodeId(), userSubscription.getRemoteNodeName(), Feed.EXPLORE
            ).forEach(postingOperations::unsetRecommended);
        }

        List<Story> stories = storyRepository.findByFeedAndTypeAndOwnerName(
            userSubscription.getNodeId(), Feed.EXPLORE, StoryType.POSTING_ADDED, userSubscription.getRemoteNodeName()
        );
        for (Story story : stories) {
            universalContext.send(new StoryDeletedLiberin(story));
            storyRepository.delete(story);
        }
    }

    public void autoSubscribe() {
        String remoteNodeName = universalContext.getOptions().getString("subscription.auto.node");
        if (ObjectUtils.isEmpty(remoteNodeName)) {
            return;
        }
        int count = userSubscriptionRepository.countByTypeAndNodeAndFeedName(
            universalContext.nodeId(), SubscriptionType.FEED, remoteNodeName, Feed.TIMELINE
        );
        if (count > 0) {
            return;
        }
        try {
            subscribe(subscription -> {
                subscription.setSubscriptionType(SubscriptionType.FEED);
                subscription.setFeedName(Feed.NEWS);
                subscription.setRemoteNodeName(remoteNodeName);
                subscription.setRemoteFeedName(Feed.TIMELINE);
                subscription.setReason(SubscriptionReason.AUTO);
            });
        } catch (OperationFailure e) {
            if (!"subscription.already-exists".equals(e.getErrorCode())) {
                throw e;
            }
        }
    }

    public void subscribeToPostingComments(String nodeName, String entryId, SubscriptionReason reason) {
        int count = userSubscriptionRepository.countByTypeAndNodeAndEntryId(
            universalContext.nodeId(), SubscriptionType.POSTING_COMMENTS, nodeName, entryId
        );
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

        contactOperations.find(nodeName).fill(subscription);
        favorOperations.addFavor(nodeName, FavorType.SUBSCRIBE_TO_COMMENTS);

        universalContext.send(new SubscriptionAddedLiberin(subscription));
        universalContext.subscriptionsUpdated();
    }

    public void deleteFeedSubscription(UserSubscription subscription) {
        UUID nodeId = subscription.getNodeId();
        String remoteNodeName = subscription.getRemoteNodeName();
        contactOperations.lockForTransaction(nodeId, remoteNodeName);
        if (userSubscriptionRepository.deleteFeedById(nodeId, subscription.getId()) == 0) {
            throw new ObjectNotFoundFailure("subscription.not-found");
        }
        decrementFeedSubscriptionCount(nodeId, remoteNodeName, 1).fill(subscription);
    }

    private Contact decrementFeedSubscriptionCount(UUID nodeId, String remoteNodeName, int removed) {
        Contact contact = contactOperations.findOrCreate(nodeId, remoteNodeName);
        contact.setFeedSubscriptionCount(Math.max(contact.getFeedSubscriptionCount() - removed, 0));
        return contact;
    }

    public void abandon(Subscription subscription) {
        if (subscription.getSubscriptionType() == SubscriptionType.FEED) {
            UUID nodeId = subscription.getNodeId();
            String remoteNodeName = subscription.getRemoteNodeName();
            contactOperations.lockForTransaction(nodeId, remoteNodeName);
            if (subscriptionRepository.updateRetryAtIfUsed(subscription.getId(), Util.farFuture()) == 0) {
                return;
            }
            int removed = userSubscriptionRepository.deleteFeedsBySource(
                nodeId, remoteNodeName, subscription.getRemoteFeedName()
            );
            if (removed > 0) {
                decrementFeedSubscriptionCount(nodeId, remoteNodeName, removed);
            }
            return;
        }

        subscriptionRepository.updateRetryAtById(subscription.getId(), Util.farFuture());
        List<UserSubscription> userSubscriptions = switch (subscription.getSubscriptionType()) {
            case POSTING, POSTING_COMMENTS -> userSubscriptionRepository.findAllByTypeAndNodeAndEntryId(
                universalContext.nodeId(),
                subscription.getSubscriptionType(),
                subscription.getRemoteNodeName(),
                subscription.getRemoteEntryId()
            );
            case PROFILE, SEARCH -> userSubscriptionRepository.findAllByType(
                universalContext.nodeId(),
                subscription.getSubscriptionType()
            );
            case USER_LIST -> userSubscriptionRepository.findAllByTypeAndNodeAndFeedName(
                universalContext.nodeId(),
                subscription.getSubscriptionType(),
                subscription.getRemoteNodeName(),
                subscription.getRemoteFeedName()
            );
            default -> null;
        };
        if (userSubscriptions != null) {
            userSubscriptionRepository.deleteAll(userSubscriptions);
        }
    }

}
