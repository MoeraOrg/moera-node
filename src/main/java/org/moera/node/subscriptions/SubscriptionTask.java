package org.moera.node.subscriptions;

import java.time.Instant;
import java.util.UUID;
import javax.inject.Inject;

import org.moera.node.api.NodeApiException;
import org.moera.node.api.NodeApiUnknownNameException;
import org.moera.node.api.NodeApiValidationException;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.Subscription;
import org.moera.node.data.SubscriptionRepository;
import org.moera.node.data.SubscriptionType;
import org.moera.node.data.UserSubscription;
import org.moera.node.media.MediaManager;
import org.moera.node.model.SubscriberDescriptionQ;
import org.moera.node.model.SubscriberInfo;
import org.moera.node.model.WhoAmI;
import org.moera.node.rest.notification.ProfileProcessor;
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
    private ProfileProcessor profileProcessor;

    @Inject
    private MediaManager mediaManager;

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
            Subscription subscription = inTransaction(
                    () -> subscriptionRepository.findById(subscriptionId).orElse(null)
            );
            if (subscription == null) {
                log.info("Subscription does not exist, ignoring");
                subscriptionManager.noAction(subscriptionId);
                return;
            }
            boolean used = subscription.getUsageCount() > 0;
            log.info("Subscription status is {}, {}",
                    subscription.getStatus().name().toLowerCase(), used ? "used" : "not used");
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
                    getNodeId(), targetNodeName, subscription.getRemoteEntryId()).orElse(null);
            if (posting != null) {
                lastEditedAt = Util.toEpochSecond(posting.getReceiverEditedAt());
            }
        }
        try {
            mediaManager.uploadPublicMedia(targetNodeName, generateCarte(targetNodeName), getAvatar());
            SubscriberDescriptionQ description = new SubscriberDescriptionQ(subscription.getSubscriptionType(),
                    subscription.getRemoteFeedName(), subscription.getRemoteEntryId(), lastEditedAt,
                    UserSubscription.getViewAllPrincipal(getOptions()).isPublic());
            SubscriberInfo subscriberInfo =
                    nodeApi.postSubscriber(targetNodeName, generateCarte(targetNodeName), description);
            subscriptionManager.succeededSubscribe(subscriptionId, subscriberInfo.getId());
        } catch (NodeApiException e) {
            error(true, e);
            if (e instanceof NodeApiValidationException
                    && ((NodeApiValidationException) e).getErrorCode()
                            .equals("subscriberDescription.postingId.not-found")) {
                subscriptionManager.subscriptionInvalid(subscription);
            } else {
                subscriptionManager.failed(subscriptionId, subscription.getCreatedAt().toInstant());
            }
            return;
        }

        try {
            WhoAmI target = nodeApi.whoAmI(targetNodeName);
            profileProcessor.updateProfileDetails(target.getNodeName(), target.getFullName(), target.getGender(),
                    target.getAvatar());
        } catch (NodeApiException e) {
            log.warn("Error updating details of node {}: {}", targetNodeName, e.getMessage());
        }
    }

    private void unsubscribe(Subscription subscription) {
        targetNodeName = subscription.getRemoteNodeName();
        try {
            nodeApi.deleteSubscriber(targetNodeName, generateCarte(targetNodeName),
                    subscription.getRemoteSubscriberId());
        } catch (NodeApiException e) {
            error(false, e);
        }
        // Ignore error, because unsubscription will happen anyway on notification
        subscriptionManager.succeededUnsubscribe(subscriptionId);
    }

    private void error(boolean subscribe, Throwable e) {
        if (e instanceof NodeApiUnknownNameException) {
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
