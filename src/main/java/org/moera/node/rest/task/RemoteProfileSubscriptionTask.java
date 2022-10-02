package org.moera.node.rest.task;

import java.util.UUID;
import javax.inject.Inject;

import org.moera.node.api.NodeApiException;
import org.moera.node.api.NodeApiUnknownNameException;
import org.moera.node.data.MediaFile;
import org.moera.node.data.SubscriberRepository;
import org.moera.node.data.Subscription;
import org.moera.node.data.SubscriptionRepository;
import org.moera.node.data.SubscriptionType;
import org.moera.node.media.MediaManager;
import org.moera.node.model.SubscriberDescriptionQ;
import org.moera.node.model.SubscriberInfo;
import org.moera.node.model.WhoAmI;
import org.moera.node.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteProfileSubscriptionTask extends Task {

    private static final Logger log = LoggerFactory.getLogger(RemoteProfileSubscriptionTask.class);

    private final String targetNodeName;

    @Inject
    private SubscriptionRepository subscriptionRepository;

    @Inject
    private SubscriberRepository subscriberRepository;

    @Inject
    private MediaManager mediaManager;

    public RemoteProfileSubscriptionTask(String targetNodeName) {
        this.targetNodeName = targetNodeName;
    }

    @Override
    protected void execute() {
        boolean subscribe = shouldSubscribe();
        try {
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
        mediaManager.uploadPublicMedia(targetNodeName, generateCarte(targetNodeName), getAvatar());
        SubscriberDescriptionQ description = new SubscriberDescriptionQ(SubscriptionType.PROFILE,
                null, null, fullName(), getAvatar());
        SubscriberInfo subscriberInfo =
                nodeApi.postSubscriber(targetNodeName, generateCarte(targetNodeName), description);

        WhoAmI target = nodeApi.whoAmI(targetNodeName);
        MediaFile targetAvatar = mediaManager.downloadPublicMedia(targetNodeName, target.getAvatar());

        Subscription subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setNodeId(nodeId);
        subscription.setSubscriptionType(SubscriptionType.PROFILE);
        subscription.setRemoteSubscriberId(subscriberInfo.getId());
        subscription.setRemoteNodeName(targetNodeName);
        subscription.setRemoteFullName(target.getFullName());
        subscription.setRemoteGender(target.getGender());
        if (targetAvatar != null) {
            subscription.setRemoteAvatarMediaFile(targetAvatar);
            subscription.setRemoteAvatarShape(target.getAvatar().getShape());
        }
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
