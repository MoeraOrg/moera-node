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
import org.moera.node.global.UniversalContext;
import org.moera.node.model.AvatarImage;
import org.moera.node.model.MediaFileInfo;
import org.moera.node.task.TaskAutowire;
import org.moera.node.util.ParametrizedLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

@Component
public class MediaManager {

    private static Logger log = LoggerFactory.getLogger(MediaManager.class);

    @Inject
    private UniversalContext universalContext;

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

    private ParametrizedLock<String> mediaFileLocks = new ParametrizedLock<>();

    public MediaFile downloadPublicMedia(String nodeName, String id, int maxSize) throws NodeApiException {
        if (id == null) {
            return null;
        }

        MediaFile mediaFile = mediaFileRepository.findById(id).orElse(null);
        if (mediaFile != null && mediaFile.isExposed()) {
            return mediaFile;
        }

        mediaFileLocks.lock(id);
        try {
            // Could appear in meantime
            mediaFile = mediaFileRepository.findById(id).orElse(null);
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
        } finally {
            mediaFileLocks.unlock(id);
        }
    }

    public MediaFile downloadPublicMedia(String nodeName, AvatarImage avatarImage)
            throws NodeApiException {

        if (avatarImage == null) {
            return null;
        }
        return downloadPublicMedia(nodeName, avatarImage.getMediaId(),
                universalContext.getOptions().getInt("avatar.max-size"));
    }

    public void asyncDownloadPublicMedia(String nodeName, AvatarImage[] avatarImages, Consumer<MediaFile[]> callback) {
        if (avatarImages == null) {
            callback.accept(null);
            return;
        }

        String[] ids = new String[avatarImages.length];
        MediaFile[] mediaFiles = new MediaFile[avatarImages.length];

        boolean all = true;
        for (int i = 0; i < avatarImages.length; i++) {
            ids[i] = avatarImages[i] != null ? avatarImages[i].getMediaId() : null;
            if (ids[i] == null) {
                mediaFiles[i] = null;
                continue;
            }
            MediaFile mediaFile = mediaFileRepository.findById(ids[i]).orElse(null);
            if (mediaFile != null && mediaFile.isExposed()) {
                mediaFiles[i] = mediaFile;
                continue;
            }
            all = false;
        }

        if (all) {
            callback.accept(mediaFiles);
            return;
        }

        var downloadTask = new PublicMediaDownloadTask(nodeName, ids, mediaFiles,
                universalContext.getOptions().getInt("avatar.max-size"), callback);
        taskAutowire.autowire(downloadTask);
        taskExecutor.execute(downloadTask);
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
