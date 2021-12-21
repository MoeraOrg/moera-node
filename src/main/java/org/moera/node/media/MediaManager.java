package org.moera.node.media;

import java.io.IOException;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.moera.node.api.NodeApi;
import org.moera.node.api.NodeApiException;
import org.moera.node.data.Avatar;
import org.moera.node.data.EntryAttachmentRepository;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.MediaFileOwnerRepository;
import org.moera.node.data.MediaFileRepository;
import org.moera.node.data.RemoteMediaCache;
import org.moera.node.data.RemoteMediaCacheRepository;
import org.moera.node.global.UniversalContext;
import org.moera.node.model.AvatarImage;
import org.moera.node.model.PostingFeatures;
import org.moera.node.model.PrivateMediaFileInfo;
import org.moera.node.model.PublicMediaFileInfo;
import org.moera.node.task.TaskAutowire;
import org.moera.node.util.ParametrizedLock;
import org.moera.node.util.Transaction;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

@Component
public class MediaManager {

    private static final Logger log = LoggerFactory.getLogger(MediaManager.class);

    private static final int REMOTE_MEDIA_CACHE_TTL = 365; // days

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
    private RemoteMediaCacheRepository remoteMediaCacheRepository;

    @Inject
    private MediaOperations mediaOperations;

    @Inject
    private PlatformTransactionManager txManager;

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
                cacheRemoteMedia(null, nodeName, id, mediaFile.getDigest(), mediaFile);
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
        int maxSize = new PostingFeatures(universalContext.getOptions()).getMediaMaxSize();
        return downloadPrivateMedia(nodeName, carte, info.getId(), info.getHash(), maxSize, entryId);
    }

    public byte[] getPrivateMediaDigest(String nodeName, String carte, PrivateMediaFileInfo info) {
        return getPrivateMediaDigest(nodeName, carte, info.getId(), info.getHash());
    }

    public byte[] getPrivateMediaDigest(String nodeName, String carte, String id, String hash) {
        RemoteMediaCache cache = remoteMediaCacheRepository
                .findByMedia(universalContext.nodeId(), nodeName, id)
                .orElse(null);
        if (cache != null) {
            return cache.getDigest();
        }

        if (hash != null) {
            MediaFile mediaFile = mediaFileRepository.findById(hash).orElse(null);
            if (mediaFile != null) {
                cacheRemoteMedia(null, nodeName, id, mediaFile.getDigest(), mediaFile);
                return mediaFile.getDigest();
            }
        }

        var tmp = mediaOperations.tmpFile();
        try {
            var tmpMedia = nodeApi.getPrivateMedia(nodeName, carte, id, tmp,
                    universalContext.getOptions().getInt("media.verification.max-size"));
            cacheRemoteMedia(null, nodeName, id, tmpMedia.getDigest(), null);
            return tmpMedia.getDigest();
        } catch (NodeApiException e) {
            return null; // TODO need more graceful approach
        } finally {
            try {
                Files.deleteIfExists(tmp.getPath());
            } catch (IOException e) {
                log.warn("Error removing temporary media file {}: {}", tmp.getPath(), e.getMessage());
            }
        }
    }

    public void cacheUploadedRemoteMedia(String remoteNodeName, String remoteMediaId, byte[] digest) {
        RemoteMediaCache cache = remoteMediaCacheRepository
                .findByMedia(universalContext.nodeId(), remoteNodeName, remoteMediaId)
                .orElse(null);
        if (cache == null) {
            cacheRemoteMedia(universalContext.nodeId(), remoteNodeName, remoteMediaId, digest, null);
        }
    }

    private void cacheRemoteMedia(UUID nodeId, String remoteNodeName, String remoteMediaId, byte[] digest,
                                  MediaFile mediaFile) {
        try {
            Transaction.execute(txManager, () -> {
                RemoteMediaCache cache = new RemoteMediaCache();
                cache.setId(UUID.randomUUID());
                cache.setNodeId(nodeId);
                cache.setRemoteNodeName(remoteNodeName);
                cache.setRemoteMediaId(remoteMediaId);
                cache.setDigest(digest);
                cache.setMediaFile(mediaFile);
                cache.setDeadline(Timestamp.from(Instant.now().plus(REMOTE_MEDIA_CACHE_TTL, ChronoUnit.DAYS)));
                remoteMediaCacheRepository.save(cache);
                return null;
            });
        } catch (DataIntegrityViolationException e) {
            // already created in another thread, ignore
        } catch (Throwable e) {
            log.error("Unexpected error occured while creating RemoteMediaCache: {}", e.getMessage());
        }
    }

    @Scheduled(fixedDelayString = "P1D")
    @Transactional
    public void purgeExpiredRemoteMedia() {
        remoteMediaCacheRepository.deleteExpired(Util.now());
    }

}
