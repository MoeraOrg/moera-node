package org.moera.node.media;

import java.io.IOException;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.UUID;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.node.types.PrivateMediaFileInfo;
import org.moera.lib.node.types.PublicMediaFileInfo;
import org.moera.lib.node.types.principal.AccessCheckers;
import org.moera.node.api.node.NodeApi;
import org.moera.node.api.node.NodeApiException;
import org.moera.node.api.node.NodeApiLocalStorageException;
import org.moera.node.data.Avatar;
import org.moera.node.data.EntryAttachmentRepository;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.MediaFileOwnerRepository;
import org.moera.node.data.MediaFileRepository;
import org.moera.node.data.RemoteMediaCache;
import org.moera.node.data.RemoteMediaCacheRepository;
import org.moera.node.global.RequestCounter;
import org.moera.node.global.UniversalContext;
import org.moera.node.model.AvatarImageUtil;
import org.moera.node.model.PostingFeaturesUtil;
import org.moera.node.util.ParametrizedLock;
import org.moera.node.util.Transaction;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MediaManager {

    private static final Logger log = LoggerFactory.getLogger(MediaManager.class);

    private static final int REMOTE_MEDIA_CACHE_TTL = 365; // days

    @Inject
    private RequestCounter requestCounter;

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
    private Transaction tx;

    @Inject
    @PersistenceContext
    private EntityManager entityManager;

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
            // Could appear in the meantime
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
                mediaFile = mediaOperations.putInPlace(id, tmpMedia.getContentType(), tmp.getPath(), null, true);
                // the entity is detached after putInPlace() transaction closed
                mediaFile = entityManager.merge(mediaFile);

                return mediaFile;
            } catch (IOException e) {
                throw new NodeApiLocalStorageException(
                        String.format("Error storing public media %s: %s", id, e.getMessage()));
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

    public MediaFile downloadPublicMedia(String nodeName, AvatarImage avatarImage) throws NodeApiException {
        if (avatarImage == null) {
            return null;
        }
        return downloadPublicMedia(nodeName, avatarImage.getMediaId(),
                universalContext.getOptions().getInt("avatar.max-size"));
    }

    public void downloadAvatar(String nodeName, AvatarImage avatarImage) throws NodeApiException {
        int maxSize = universalContext.getOptions().getInt("avatar.max-size");

        String id = avatarImage != null ? avatarImage.getMediaId() : null;
        if (id == null) {
            return;
        }

        if (AvatarImageUtil.getMediaFile(avatarImage) != null) {
            return;
        }
        MediaFile mediaFile = mediaFileRepository.findById(id).orElse(null);
        if (mediaFile != null && mediaFile.isExposed()) {
            AvatarImageUtil.setMediaFile(avatarImage, mediaFile);
            return;
        }

        AvatarImageUtil.setMediaFile(avatarImage, downloadPublicMedia(nodeName, id, maxSize));
    }

    public void downloadAvatars(String nodeName, AvatarImage[] avatarImages) throws NodeApiException {
        if (avatarImages != null) {
            for (AvatarImage avatarImage : avatarImages) {
                downloadAvatar(nodeName, avatarImage);
            }
        }
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
            Collection<MediaFileOwner> mediaFileOwners = mediaFileOwnerRepository
                    .findByAdminFile(universalContext.nodeId(), mediaFileId);
            for (MediaFileOwner mediaFileOwner : mediaFileOwners) {
                int count = entryAttachmentRepository.countByEntryIdAndMedia(
                        universalContext.nodeId(), entryId, mediaFileOwner.getId());
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

            try {
                MediaFile mediaFile = remoteMediaCacheRepository.findDownloadedMedia(nodeName, id).stream()
                        .findFirst()
                        .orElse(null);
                if (mediaFile != null) {
                    if (!mediaFile.getId().equals(mediaFileId)) {
                        log.warn("Media {} has hash {} instead of {}", id, mediaFile.getId(), mediaFileId);
                        return null;
                    }
                } else {
                    var tmp = mediaOperations.tmpFile();
                    try {
                        var tmpMedia = nodeApi.getPrivateMedia(nodeName, carte, id, tmp, maxSize);
                        if (!tmpMedia.getMediaFileId().equals(mediaFileId)) {
                            log.warn("Media {} has hash {} instead of {}", id, tmpMedia.getMediaFileId(), mediaFileId);
                            return null;
                        }
                        mediaFile = mediaOperations.putInPlace(
                                mediaFileId, tmpMedia.getContentType(), tmp.getPath(), null, false);
                    } finally {
                        try {
                            Files.deleteIfExists(tmp.getPath());
                        } catch (IOException e) {
                            log.warn("Error removing temporary media file {}: {}", tmp.getPath(), e.getMessage());
                        }
                    }
                    cacheRemoteMedia(null, nodeName, id, mediaFile.getDigest(), mediaFile);
                }
                // Now we are sure that the remote node owns the file with mediaFileId hash, so we can use it
                // for MediaFileOwner

                mediaFile = entityManager.merge(mediaFile); // entity is detached after putInPlace() transaction closed
                mediaFileOwner = mediaOperations.own(mediaFile, null);

                return mediaFileOwner;
            } catch (IOException e) {
                throw new NodeApiLocalStorageException(
                        String.format("Error storing private media %s: %s", id, e.getMessage()));
            }
        } finally {
            mediaFileLocks.unlock(mediaFileId);
        }
    }

    public MediaFileOwner downloadPrivateMedia(
        String nodeName, String carte, PrivateMediaFileInfo info, UUID entryId
    ) throws NodeApiException {
        int maxSize = PostingFeaturesUtil.build(universalContext.getOptions(), AccessCheckers.ADMIN).getMediaMaxSize();
        return downloadPrivateMedia(nodeName, carte, info.getId(), info.getHash(), maxSize, entryId);
    }

    public byte[] getPrivateMediaDigest(String nodeName, String carte, PrivateMediaFileInfo info) {
        return getPrivateMediaDigest(nodeName, carte, info.getId(), info.getHash());
    }

    public byte[] getPrivateMediaDigest(String nodeName, String carte, String id, String hash) {
        RemoteMediaCache cache = remoteMediaCacheRepository
                .findByMedia(universalContext.nodeId(), nodeName, id)
                .stream()
                .findFirst()
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
        boolean cached = !remoteMediaCacheRepository
                .findByMedia(universalContext.nodeId(), remoteNodeName, remoteMediaId).isEmpty();
        if (!cached) {
            cacheRemoteMedia(universalContext.nodeId(), remoteNodeName, remoteMediaId, digest, null);
        }
    }

    private void cacheRemoteMedia(UUID nodeId, String remoteNodeName, String remoteMediaId, byte[] digest,
                                  MediaFile mediaFile) {
        try {
            tx.executeWrite(() -> {
                RemoteMediaCache cache = new RemoteMediaCache();
                cache.setId(UUID.randomUUID());
                cache.setNodeId(nodeId);
                cache.setRemoteNodeName(remoteNodeName);
                cache.setRemoteMediaId(remoteMediaId);
                cache.setDigest(digest);
                cache.setMediaFile(mediaFile);
                cache.setDeadline(Timestamp.from(Instant.now().plus(REMOTE_MEDIA_CACHE_TTL, ChronoUnit.DAYS)));
                remoteMediaCacheRepository.save(cache);
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
        try (var ignored = requestCounter.allot()) {
            log.info("Purging remote media cache");

            remoteMediaCacheRepository.deleteExpired(Util.now());
        }
    }

}
