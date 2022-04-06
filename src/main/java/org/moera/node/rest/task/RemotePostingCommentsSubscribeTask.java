package org.moera.node.rest.task;

import java.util.UUID;
import javax.inject.Inject;

import org.moera.node.api.NodeApiUnknownNameException;
import org.moera.node.data.MediaFile;
import org.moera.node.data.Subscription;
import org.moera.node.data.SubscriptionReason;
import org.moera.node.data.SubscriptionRepository;
import org.moera.node.data.SubscriptionType;
import org.moera.node.instant.PostingInstants;
import org.moera.node.liberin.model.SubscriptionAddedLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.SubscriberDescriptionQ;
import org.moera.node.model.SubscriberInfo;
import org.moera.node.model.WhoAmI;
import org.moera.node.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemotePostingCommentsSubscribeTask extends Task {

    private static final Logger log = LoggerFactory.getLogger(RemotePostingCommentsSubscribeTask.class);

    private final String targetNodeName;
    private final String postingId;
    private final SubscriptionReason reason;

    @Inject
    private SubscriptionRepository subscriptionRepository;

    @Inject
    private PostingInstants postingInstants;

    @Inject
    private MediaManager mediaManager;

    public RemotePostingCommentsSubscribeTask(String targetNodeName, String postingId, SubscriptionReason reason) {
        this.targetNodeName = targetNodeName;
        this.postingId = postingId;
        this.reason = reason;
    }

    @Override
    protected void execute() {
        try {
            boolean subscribed = !subscriptionRepository.findAllByTypeAndNodeAndEntryId(nodeId,
                    SubscriptionType.POSTING_COMMENTS, targetNodeName, postingId).isEmpty();
            if (subscribed) {
                return;
            }

            WhoAmI target = nodeApi.whoAmI(targetNodeName);
            MediaFile targetAvatar = mediaManager.downloadPublicMedia(targetNodeName, target.getAvatar());

            SubscriberDescriptionQ description = new SubscriberDescriptionQ(SubscriptionType.POSTING_COMMENTS,
                    null, postingId, fullName(), getAvatar());
            SubscriberInfo subscriberInfo =
                    nodeApi.postSubscriber(targetNodeName, generateCarte(targetNodeName), description);

            Subscription subscription = new Subscription();
            subscription.setId(UUID.randomUUID());
            subscription.setNodeId(nodeId);
            subscription.setSubscriptionType(SubscriptionType.POSTING_COMMENTS);
            subscription.setRemoteSubscriberId(subscriberInfo.getId());
            subscription.setRemoteNodeName(targetNodeName);
            subscription.setRemoteFullName(target.getFullName());
            if (targetAvatar != null) {
                subscription.setRemoteAvatarMediaFile(targetAvatar);
                subscription.setRemoteAvatarShape(target.getAvatar().getShape());
            }
            subscription.setRemoteEntryId(postingId);
            subscription.setReason(reason);
            subscription = subscriptionRepository.save(subscription);
            send(new SubscriptionAddedLiberin(subscription));
            success();
        } catch (Exception e) {
            error(e);
        }
    }

    private void success() {
        log.info("Succeeded to subscribe to comments to posting {} at node {}", postingId, targetNodeName);
    }

    private void error(Throwable e) {
        if (e instanceof NodeApiUnknownNameException) {
            log.error("Cannot find a node {}", targetNodeName);
        } else {
            log.error("Error subscribing to comments to posting {} at node {}: {}", postingId, targetNodeName,
                    e.getMessage());
        }

        PostingInfo postingInfo = null;
        try {
            postingInfo = nodeApi.getPosting(targetNodeName, postingId);
            if (postingInfo.getOwnerAvatar() != null) {
                MediaFile mediaFile = mediaManager.downloadPublicMedia(targetNodeName, postingInfo.getOwnerAvatar());
                postingInfo.getOwnerAvatar().setMediaFile(mediaFile);
            }
        } catch (Exception ex) {
            // ignore
        }
        postingInstants.subscribingToCommentsFailed(postingId, postingInfo);
    }

}
