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
    private String[] mediaIds;
    private MediaFile[] mediaFiles;
    private int maxSize;
    private Consumer<MediaFile[]> callback;

    @Inject
    private MediaManager mediaManager;

    public PublicMediaDownloadTask(String targetNodeName, String[] mediaIds, MediaFile[] mediaFiles, int maxSize,
                                   Consumer<MediaFile[]> callback) {
        this.targetNodeName = targetNodeName;
        this.mediaIds = mediaIds;
        this.mediaFiles = mediaFiles;
        this.maxSize = maxSize;
        this.callback = callback;
    }

    @Override
    protected void execute() {
        String mediaId = null;
        try {
            for (int i = 0; i < mediaIds.length; i++) {
                if (mediaIds[i] == null || mediaFiles[i] != null) {
                    continue;
                }
                mediaId = mediaIds[i];
                mediaFiles[i] = mediaManager.downloadPublicMedia(targetNodeName, mediaId, maxSize);
                success(mediaId);
            }
            callback.accept(mediaFiles);
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
