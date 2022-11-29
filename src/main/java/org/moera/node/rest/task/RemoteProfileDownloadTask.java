package org.moera.node.rest.task;

import javax.inject.Inject;

import org.moera.node.api.NodeApiUnknownNameException;
import org.moera.node.data.MediaFile;
import org.moera.node.liberin.model.RemoteNodeAvatarChangedLiberin;
import org.moera.node.liberin.model.RemoteNodeFullNameChangedLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.model.AvatarImage;
import org.moera.node.model.WhoAmI;
import org.moera.node.operations.RemoteProfileOperations;
import org.moera.node.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteProfileDownloadTask extends Task {

    private static final Logger log = LoggerFactory.getLogger(RemoteProfileDownloadTask.class);

    private final String targetNodeName;

    @Inject
    private RemoteProfileOperations remoteProfileOperations;

    @Inject
    private MediaManager mediaManager;

    public RemoteProfileDownloadTask(String targetNodeName) {
        this.targetNodeName = targetNodeName;
    }

    @Override
    protected void execute() {
        try {
            WhoAmI whoAmI = nodeApi.whoAmI(targetNodeName);
            if (whoAmI.getFullName() != null || whoAmI.getGender() != null) {
                inTransaction(() -> {
                    remoteProfileOperations.updateDetails(targetNodeName, whoAmI.getFullName(), whoAmI.getGender());
                    return null;
                });
                send(new RemoteNodeFullNameChangedLiberin(targetNodeName, whoAmI.getFullName()));
            }
            AvatarImage targetAvatar = whoAmI.getAvatar();
            MediaFile mediaFile = mediaManager.downloadPublicMedia(targetNodeName, targetAvatar);
            if (mediaFile != null) {
                inTransaction(() -> {
                    remoteProfileOperations.updateAvatar(targetNodeName, mediaFile, targetAvatar.getShape());
                    return null;
                });
                send(new RemoteNodeAvatarChangedLiberin(
                        targetNodeName, new AvatarImage(mediaFile, targetAvatar.getShape())));
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
