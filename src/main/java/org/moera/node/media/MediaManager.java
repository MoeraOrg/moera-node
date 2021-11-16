package org.moera.node.media;

import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.moera.node.api.NodeApi;
import org.moera.node.api.NodeApiException;
import org.moera.node.data.Avatar;
import org.moera.node.data.EntryAttachmentRepository;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.MediaFileOwnerRepository;
import org.moera.node.data.MediaFileRepository;
import org.moera.node.global.UniversalContext;
import org.moera.node.model.AvatarImage;
import org.moera.node.model.PrivateMediaFileInfo;
import org.moera.node.model.PublicMediaFileInfo;
import org.moera.node.task.TaskAutowire;
import org.moera.node.util.ParametrizedLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

@Component
public class MediaManager {

    private static final Logger log = LoggerFactory.getLogger(MediaManager.class);

    @Inject
    private UniversalContext universalContext;

    @Inject
    private NodeApi nodeApi;

    @Inject
    private MediaFileRepository mediaFileRepository;

    @Inject
    private MediaFileOwnerRepository mediaFileOwnerRepository;

    @Inject
    private EntryAttachmentRepository entryAttachmentRepository;

    @Inject
    private MediaOperations mediaOperations;

    @Inject
    private EntityManager entityManager;

    @Inject
    @Qualifier("remoteTaskExecutor")
    private TaskExecutor taskExecutor;

    @Inject
    private TaskAutowire taskAutowire;

    private final ParametrizedLock<String> mediaFileLocks = new ParametrizedLock<>();

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
                if (!tmpMedia.getMediaFileId().equals(id)) {
                    log.warn("Public media {} has hash {}", id, tmpMedia.getMediaFileId());
                    return null;
                }
                mediaFile = mediaOperations.putInPlace(id, tmpMedia.getContentType(), tmp.getPath(), null);
                mediaFile = entityManager.merge(mediaFile); // entity is detached after putInPlace() transaction closed
                mediaFile.setExposed(true);

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
        PublicMediaFileInfo info = nodeApi.getPublicMediaInfo(nodeName, mediaFile.getId());
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

    private MediaFileOwner findAttachedMedia(String mediaFileId, UUID entryId) {
        if (entryId != null) {
            MediaFileOwner mediaFileOwner = mediaFileOwnerRepository
                    .findByAdminFile(universalContext.nodeId(), mediaFileId).orElse(null);
            if (mediaFileOwner != null) {
                int count = entryAttachmentRepository.countByEntryIdAndMedia(universalContext.nodeId(), entryId,
                        mediaFileOwner.getId());
                if (count > 0) {
                    return mediaFileOwner;
                }
            }
        }

        return null;
    }

    public MediaFileOwner downloadPrivateMedia(String nodeName, String carte, String id, String mediaFileId,
                                               int maxSize, UUID entryId) throws NodeApiException {
        if (id == null) {
            return null;
        }

        MediaFileOwner mediaFileOwner = findAttachedMedia(mediaFileId, entryId);
        if (mediaFileOwner != null) {
            return mediaFileOwner;
        }

        mediaFileLocks.lock(mediaFileId);
        try {
            // Could appear in meantime
            mediaFileOwner = findAttachedMedia(mediaFileId, entryId);
            if (mediaFileOwner != null) {
                return mediaFileOwner;
            }

            var tmp = mediaOperations.tmpFile();
            try {
                var tmpMedia = nodeApi.getPrivateMedia(nodeName, carte, id, tmp, maxSize);
                if (!tmpMedia.getMediaFileId().equals(mediaFileId)) {
                    log.warn("Media {} has hash {} instead of {}", id, tmpMedia.getMediaFileId(), mediaFileId);
                    return null;
                }
                MediaFile mediaFile = mediaOperations.putInPlace(
                        mediaFileId, tmpMedia.getContentType(), tmp.getPath(), null);
                // Now we are sure that the remote node owns the file with mediaFileId hash, so we can use
                // our MediaFileOwner, if exists
                mediaFileOwner = mediaFileOwnerRepository
                        .findByAdminFile(universalContext.nodeId(), mediaFileId).orElse(null);
                if (mediaFileOwner == null) {
                    // entity is detached after putInPlace() transaction closed
                    mediaFile = entityManager.merge(mediaFile);
                    mediaFileOwner = mediaOperations.own(mediaFile, null);
                }

                return mediaFileOwner;
            } catch (IOException e) {
                throw new NodeApiException(String.format("Error storing private media %s: %s", id, e.getMessage()));
            } finally {
                try {
                    Files.deleteIfExists(tmp.getPath());
                } catch (IOException e) {
                    log.warn("Error removing temporary media file {}: {}", tmp.getPath(), e.getMessage());
                }
            }
        } finally {
            mediaFileLocks.unlock(mediaFileId);
        }
    }

    public MediaFileOwner downloadPrivateMedia(String nodeName, String carte, PrivateMediaFileInfo info,
                                               UUID entryId) throws NodeApiException {
        return downloadPrivateMedia(nodeName, carte, info.getId(), info.getHash(),
                universalContext.getOptions().getInt("posting.media.max-size"), entryId);
    }

}
