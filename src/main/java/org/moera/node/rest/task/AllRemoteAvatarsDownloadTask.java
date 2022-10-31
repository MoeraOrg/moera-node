package org.moera.node.rest.task;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;

import org.moera.node.api.NodeApiUnknownNameException;
import org.moera.node.data.ContactRepository;
import org.moera.node.data.DomainUpgradeRepository;
import org.moera.node.data.MediaFile;
import org.moera.node.data.Subscriber;
import org.moera.node.data.SubscriberRepository;
import org.moera.node.data.SubscriptionType;
import org.moera.node.data.UpgradeType;
import org.moera.node.data.UserSubscription;
import org.moera.node.data.UserSubscriptionRepository;
import org.moera.node.media.MediaManager;
import org.moera.node.model.AvatarImage;
import org.moera.node.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AllRemoteAvatarsDownloadTask extends Task {

    private static final Logger log = LoggerFactory.getLogger(AllRemoteAvatarsDownloadTask.class);

    @Inject
    private UserSubscriptionRepository userSubscriptionRepository;

    @Inject
    private SubscriberRepository subscriberRepository;

    @Inject
    private ContactRepository contactRepository;

    @Inject
    private DomainUpgradeRepository domainUpgradeRepository;

    @Inject
    private MediaManager mediaManager;

    public AllRemoteAvatarsDownloadTask() {
    }

    @Override
    protected void execute() {
        Set<String> targetNodeNames = getTargetNodeNames();
        for (String targetNodeName : targetNodeNames) {
            Duration delay = Duration.ofSeconds(30);
            for (int i = 0; i < 5; i++) {
                try {
                    download(targetNodeName);
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
                domainUpgradeRepository.deleteByTypeAndNode(UpgradeType.AVATAR_DOWNLOAD, nodeId);
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
        userSubscriptionRepository.findAllByType(nodeId, SubscriptionType.FEED).stream()
                .map(UserSubscription::getRemoteNodeName)
                .forEach(nodeNames::add);
        return nodeNames;
    }

    private void download(String targetNodeName) throws Throwable {
        AvatarImage targetAvatar = nodeApi.whoAmI(targetNodeName).getAvatar();
        MediaFile mediaFile = mediaManager.downloadPublicMedia(targetNodeName, targetAvatar);
        if (mediaFile != null) {
            inTransaction(() -> {
                subscriberRepository.updateRemoteAvatar(nodeId, targetNodeName, mediaFile, targetAvatar.getShape());
                userSubscriptionRepository.updateRemoteAvatar(nodeId, targetNodeName, mediaFile,
                        targetAvatar.getShape());
                contactRepository.updateRemoteAvatar(nodeId, targetNodeName, mediaFile, targetAvatar.getShape());
                return null;
            });
        }
    }

    private void success(String targetNodeName) {
        log.info("Succeeded to download the avatar of node {}", targetNodeName);
    }

    private void error(String targetNodeName, Throwable e) {
        if (e instanceof NodeApiUnknownNameException) {
            log.error("Cannot find a node {}", targetNodeName);
        } else {
            log.error("Error downloading the avatar of node {}: {}", targetNodeName, e.getMessage());
        }
    }

}
