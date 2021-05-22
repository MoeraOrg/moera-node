package org.moera.node.media;

import java.io.IOException;
import java.nio.file.Files;
import java.util.function.Consumer;
import javax.inject.Inject;

import org.moera.node.api.NodeApi;
import org.moera.node.api.NodeApiException;
import org.moera.node.data.Avatar;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileRepository;
import org.moera.node.model.AvatarImage;
import org.moera.node.model.MediaFileInfo;
import org.moera.node.task.TaskAutowire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

@Component
public class MediaManager {

    private static Logger log = LoggerFactory.getLogger(MediaManager.class);

    @Inject
    private NodeApi nodeApi;

    @Inject
    private MediaFileRepository mediaFileRepository;

    @Inject
    private MediaOperations mediaOperations;

    @Inject
    @Qualifier("remoteTaskExecutor")
    private TaskExecutor taskExecutor;

    @Inject
    private TaskAutowire taskAutowire;

    public MediaFile downloadPublicMedia(String nodeName, String id, int maxSize) throws NodeApiException {
        if (id == null) {
            return null;
        }

        MediaFile mediaFile = mediaFileRepository.findById(id).orElse(null);
        if (mediaFile != null && mediaFile.isExposed()) {
            return mediaFile;
        }

        var tmp = mediaOperations.tmpFile();
        try {
            var tmpMedia = nodeApi.getPublicMedia(nodeName, id, tmp, maxSize);
            mediaFile = mediaOperations.putInPlace(id, tmpMedia.getContentType(), tmp.getPath());
            mediaFile.setExposed(true);
            mediaFile = mediaFileRepository.save(mediaFile);

            return mediaFile;
        } catch (IOException e) {
            throw new NodeApiException(String.format("Error storing public media %s: %s", id, e.getMessage()));
        } finally {
            try {
                Files.deleteIfExists(tmp.getPath());
            } catch (IOException e) {
                log.warn("Error removing temporary media file {}: {}", tmp.getPath(), e.getMessage());
            }
        }
    }

    public MediaFile downloadPublicMedia(String nodeName, AvatarImage avatarImage, int maxSize)
            throws NodeApiException {

        if (avatarImage == null) {
            return null;
        }
        return downloadPublicMedia(nodeName, avatarImage.getMediaId(), maxSize);
    }

    public void asyncDownloadPublicMedia(String nodeName, String id, int maxSize, Consumer<MediaFile> callback) {
        if (id == null) {
            callback.accept(null);
            return;
        }

        /*MediaFile mediaFile = mediaFileRepository.findById(id).orElse(null);
        if (mediaFile != null && mediaFile.isExposed()) {
            callback.accept(mediaFile);
            return;
        }*/

        var downloadTask = new PublicMediaDownloadTask(nodeName, id, maxSize, callback);
        taskAutowire.autowire(downloadTask);
        taskExecutor.execute(downloadTask);
    }

    public void asyncDownloadPublicMedia(String nodeName, AvatarImage avatarImage, int maxSize,
                                         Consumer<MediaFile> callback) {
        if (avatarImage == null) {
            callback.accept(null);
            return;
        }
        asyncDownloadPublicMedia(nodeName, avatarImage.getMediaId(), maxSize, callback);
    }

    public void uploadPublicMedia(String nodeName, String carte, MediaFile mediaFile) throws NodeApiException {
        if (mediaFile == null) {
            return;
        }
        MediaFileInfo info = nodeApi.getPublicMediaInfo(nodeName, mediaFile.getId());
        if (info != null) {
            return;
        }
        nodeApi.postPublicMedia(nodeName, carte, mediaFile);
    }

    public void uploadPublicMedia(String nodeName, String carte, Avatar avatar) throws NodeApiException {
        if (avatar == null) {
            return;
        }
        uploadPublicMedia(nodeName, carte, avatar.getMediaFile());
    }

}
