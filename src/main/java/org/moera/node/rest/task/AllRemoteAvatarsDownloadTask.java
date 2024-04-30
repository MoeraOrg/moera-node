package org.moera.node.rest.task;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;

import org.moera.node.api.node.NodeApiUnknownNameException;
import org.moera.node.data.Contact;
import org.moera.node.data.ContactRepository;
import org.moera.node.data.DomainUpgradeRepository;
import org.moera.node.data.MediaFile;
import org.moera.node.data.UpgradeType;
import org.moera.node.media.MediaManager;
import org.moera.node.model.AvatarImage;
import org.moera.node.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AllRemoteAvatarsDownloadTask extends Task {

    private static final Logger log = LoggerFactory.getLogger(AllRemoteAvatarsDownloadTask.class);

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
                    // ignore
                }
                delay = delay.multipliedBy(2);
            }
        }
        tx.executeWriteQuietly(
            () -> domainUpgradeRepository.deleteByTypeAndNode(UpgradeType.AVATAR_DOWNLOAD, nodeId),
            e -> log.error("Error deleting domain upgrade record: {}", e.getMessage())
        );
    }

    private Set<String> getTargetNodeNames() {
        return contactRepository.findAllByNodeId(nodeId).stream()
                .map(Contact::getRemoteNodeName)
                .collect(Collectors.toSet());
    }

    private void download(String targetNodeName) throws Exception {
        AvatarImage targetAvatar = nodeApi.whoAmI(targetNodeName).getAvatar();
        MediaFile mediaFile = mediaManager.downloadPublicMedia(targetNodeName, targetAvatar);
        if (mediaFile != null) {
            tx.executeWrite(() ->
                    contactRepository.updateRemoteAvatar(nodeId, targetNodeName, mediaFile, targetAvatar.getShape()));
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
