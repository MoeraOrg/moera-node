package org.moera.node.subscriptions;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import jakarta.inject.Inject;

import org.moera.lib.crypto.CryptoException;
import org.moera.lib.node.exception.MoeraNodeApiNotFoundException;
import org.moera.lib.node.exception.MoeraNodeException;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.SubscriberDescription;
import org.moera.lib.node.types.SubscriberInfo;
import org.moera.lib.node.types.SubscriptionType;
import org.moera.node.api.naming.NamingNotAvailableException;
import org.moera.node.api.node.MoeraNodeUnknownNameException;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.Subscription;
import org.moera.node.data.SubscriptionRepository;
import org.moera.node.data.UserSubscription;
import org.moera.node.media.MediaManager;
import org.moera.node.model.SubscriberDescriptionUtil;
import org.moera.node.rest.notification.ProfileUpdateJob;
import org.moera.node.task.Jobs;
import org.moera.node.task.Task;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubscriptionTask extends Task {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionTask.class);

    private final UUID subscriptionId;
    private String targetNodeName;

    @Inject
    private SubscriptionRepository subscriptionRepository;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private SubscriptionManager subscriptionManager;

    @Inject
    private MediaManager mediaManager;

    @Inject
    private Jobs jobs;

    public SubscriptionTask(UUID subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public UUID getSubscriptionId() {
        return subscriptionId;
    }

    @Override
    protected void execute() {
        log.info("Establishing/closing subscription {}", subscriptionId);
        try {
            Subscription subscription = tx.executeRead(
                () -> subscriptionRepository.findById(subscriptionId).orElse(null)
            );
            if (subscription == null) {
                log.info("Subscription does not exist, ignoring");
                subscriptionManager.noAction(subscriptionId);
                return;
            }
            boolean used = subscription.getUsageCount() > 0;
            log.info(
                "Subscription status is {}, {}",
                subscription.getStatus().name().toLowerCase(),
                used ? "used" : "not used"
            );
            switch (subscription.getStatus()) {
                case PENDING:
                    if (used) {
                        subscribe(subscription);
                    } else {
                        subscriptionManager.succeededUnsubscribe(subscriptionId);
                    }
                    break;
                case ESTABLISHED:
                    if (used) {
                        subscriptionManager.noAction(subscriptionId);
                    } else {
                        unsubscribe(subscription);
                    }
                    break;
                default:
                    subscriptionManager.noAction(subscriptionId);
                    break;
            }
        } catch (Throwable e) {
            log.error("Error fetching subscription", e);
        }
    }

    private void subscribe(Subscription subscription) {
        targetNodeName = subscription.getRemoteNodeName();
        long lastEditedAt = Instant.now().getEpochSecond();
        if (subscription.getSubscriptionType() == SubscriptionType.POSTING) {
            Posting posting = postingRepository.findByReceiverId(
                getNodeId(), targetNodeName, subscription.getRemoteEntryId()
            ).orElse(null);
            if (posting != null) {
                lastEditedAt = Util.toEpochSecond(posting.getReceiverEditedAt());
            }
        }
        try {
            mediaManager.uploadPublicMedia(
                targetNodeName, generateCarte(targetNodeName, Scope.UPLOAD_PUBLIC_MEDIA), getAvatar()
            );
            SubscriberDescription description = SubscriberDescriptionUtil.build(
                subscription.getSubscriptionType(),
                subscription.getRemoteFeedName(),
                subscription.getRemoteEntryId(),
                lastEditedAt,
                UserSubscription.getViewAllPrincipal(getOptions()).isPublic()
            );
            SubscriberInfo subscriberInfo = nodeApi
                .at(targetNodeName, generateCarte(targetNodeName, Scope.SUBSCRIBE))
                .createSubscriber(description);
            subscriptionManager.succeededSubscribe(subscriptionId, subscriberInfo.getId());
        } catch (CryptoException | MoeraNodeException | NamingNotAvailableException e) {
            error(true, e);
            if (
                e instanceof MoeraNodeApiNotFoundException ve && Objects.equals(ve.getErrorCode(), "posting.not-found")
            ) {
                subscriptionManager.subscriptionInvalid(subscription);
            } else {
                subscriptionManager.failed(subscription);
            }
            return;
        }

        jobs.run(ProfileUpdateJob.class, new ProfileUpdateJob.Parameters(targetNodeName), nodeId);
    }

    private void unsubscribe(Subscription subscription) {
        targetNodeName = subscription.getRemoteNodeName();
        try {
            nodeApi
                .at(targetNodeName, generateCarte(targetNodeName, Scope.SUBSCRIBE))
                .deleteSubscriber(subscription.getRemoteSubscriberId());
        } catch (CryptoException | MoeraNodeException e) {
            error(false, e);
        }
        // Ignore error, because unsubscription will happen anyway on notification
        subscriptionManager.succeededUnsubscribe(subscriptionId);
    }

    private void error(boolean subscribe, Throwable e) {
        if (e instanceof MoeraNodeUnknownNameException) {
            log.error("Cannot find a node {}", targetNodeName);
        } else {
            if (subscribe) {
                log.error("Error subscribing to node {}: {}", targetNodeName, e.getMessage());
            } else {
                log.error("Error unsubscribing from node {}: {}", targetNodeName, e.getMessage());
            }
        }
    }

}
