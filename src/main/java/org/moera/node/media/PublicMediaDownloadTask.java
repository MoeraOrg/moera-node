package org.moera.node.media;

import javax.inject.Inject;

import org.moera.node.api.NodeApiUnknownNameException;
import org.moera.node.model.AvatarImage;
import org.moera.node.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PublicMediaDownloadTask extends Task {

    private static final Logger log = LoggerFactory.getLogger(PublicMediaDownloadTask.class);

    private final String targetNodeName;
    private final AvatarImage[] avatars;
    private final int maxSize;
    private final Runnable callback;

    @Inject
    private MediaManager mediaManager;

    public PublicMediaDownloadTask(String targetNodeName, AvatarImage[] avatars, int maxSize, Runnable callback) {
        this.targetNodeName = targetNodeName;
        this.avatars = avatars;
        this.maxSize = maxSize;
        this.callback = callback;
    }

    @Override
    protected void execute() {
        String mediaId = null;
        try {
            for (AvatarImage avatar : avatars) {
                if (avatar == null || avatar.getMediaId() == null || avatar.getMediaFile() != null) {
                    continue;
                }
                mediaId = avatar.getMediaId();
                avatar.setMediaFile(mediaManager.downloadPublicMedia(targetNodeName, mediaId, maxSize));
                success(mediaId);
            }
            callback.run();
        } catch (Throwable e) {
            error(mediaId, e);
        }
    }

    private void success(String mediaId) {
        log.info("Succeeded to download public media {} from node {}", mediaId, targetNodeName);
    }

    private void error(String mediaId, Throwable e) {
        if (e instanceof NodeApiUnknownNameException) {
            log.error("Cannot find a node {}", targetNodeName);
        } else {
            log.error("Error downloading public media {} from node {}: {}", mediaId, targetNodeName, e.getMessage());
        }
    }

}
