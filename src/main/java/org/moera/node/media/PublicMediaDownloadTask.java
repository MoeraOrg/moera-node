package org.moera.node.media;

import java.util.function.Consumer;
import javax.inject.Inject;

import org.moera.node.api.NodeApiUnknownNameException;
import org.moera.node.data.MediaFile;
import org.moera.node.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PublicMediaDownloadTask extends Task {

    private static Logger log = LoggerFactory.getLogger(PublicMediaDownloadTask.class);

    private String targetNodeName;
    private String mediaId;
    private int maxSize;
    private Consumer<MediaFile> callback;

    @Inject
    private MediaManager mediaManager;

    public PublicMediaDownloadTask(String targetNodeName, String mediaId, int maxSize, Consumer<MediaFile> callback) {
        this.targetNodeName = targetNodeName;
        this.mediaId = mediaId;
        this.maxSize = maxSize;
        this.callback = callback;
    }

    @Override
    protected void execute() {
        try {
            MediaFile mediaFile = mediaManager.downloadPublicMedia(targetNodeName, mediaId, maxSize);
            callback.accept(mediaFile);
            success();
        } catch (Throwable e) {
            error(e);
        }
    }

    private void success() {
        log.info("Succeeded to download public media {} from node {}", mediaId, targetNodeName);
    }

    private void error(Throwable e) {
        if (e instanceof NodeApiUnknownNameException) {
            log.error("Cannot find a node {}", targetNodeName);
        } else {
            log.error("Error downloading public media {} from node {}: {}", mediaId, targetNodeName, e.getMessage());
        }
    }

}
