package org.moera.node.rest.task;

import javax.inject.Inject;

import org.moera.node.api.NodeApiUnknownNameException;
import org.moera.node.data.ContactRepository;
import org.moera.node.data.MediaFile;
import org.moera.node.data.SubscriberRepository;
import org.moera.node.data.SubscriptionRepository;
import org.moera.node.media.MediaManager;
import org.moera.node.model.AvatarImage;
import org.moera.node.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteAvatarDownloadTask extends Task {

    private static Logger log = LoggerFactory.getLogger(RemoteAvatarDownloadTask.class);

    private String targetNodeName;

    @Inject
    private SubscriptionRepository subscriptionRepository;

    @Inject
    private SubscriberRepository subscriberRepository;

    @Inject
    private ContactRepository contactRepository;

    @Inject
    private MediaManager mediaManager;

    public RemoteAvatarDownloadTask(String targetNodeName) {
        this.targetNodeName = targetNodeName;
    }

    @Override
    protected void execute() {
        try {
            AvatarImage targetAvatar = nodeApi.whoAmI(targetNodeName).getAvatar();
            MediaFile mediaFile = mediaManager.downloadPublicMedia(targetNodeName, targetAvatar,
                    getOptions().getInt("posting.media.max-size"));
            if (mediaFile != null) {
                inTransaction(() -> {
                    subscriberRepository.updateRemoteAvatar(nodeId, targetNodeName, mediaFile, targetAvatar.getShape());
                    subscriptionRepository.updateRemoteAvatar(nodeId, targetNodeName, mediaFile,
                            targetAvatar.getShape());
                    contactRepository.updateRemoteAvatar(nodeId, targetNodeName, mediaFile, targetAvatar.getShape());
                    return null;
                });
            }
            success();
        } catch (Throwable e) {
            error(e);
        }
    }

    private void success() {
        log.info("Succeeded to download the avatar of node {}", targetNodeName);
    }

    private void error(Throwable e) {
        if (e instanceof NodeApiUnknownNameException) {
            log.error("Cannot find a node {}", targetNodeName);
        } else {
            log.error("Error downloading the avatar of node {}: {}", targetNodeName, e.getMessage());
        }
    }

}
