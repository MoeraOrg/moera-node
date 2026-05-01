package org.moera.node.media;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import okhttp3.ResponseBody;
import org.moera.lib.node.exception.MoeraNodeException;
import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.node.types.PrivateMediaFileInfo;
import org.moera.lib.node.types.PublicMediaFileInfo;
import org.moera.lib.node.types.principal.AccessCheckers;
import org.moera.node.api.node.MoeraNodeLocalStorageException;
import org.moera.node.api.node.NodeApi;
import org.moera.node.config.Config;
import org.moera.node.data.Avatar;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.MediaFileOwnerRepository;
import org.moera.node.data.MediaFileRepository;
import org.moera.node.data.RemoteMediaCache;
import org.moera.node.data.RemoteMediaCacheRepository;
import org.moera.node.data.RemoteMediaError;
import org.moera.node.global.UniversalContext;
import org.moera.node.model.AvatarImageUtil;
import org.moera.node.model.PostingFeaturesUtil;
import org.moera.node.util.DigestingOutputStream;
import org.moera.node.util.ParametrizedLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class MediaManager {

    private static final Logger log = LoggerFactory.getLogger(MediaManager.class);

    @Inject
    private Config config;

    @Inject
    private UniversalContext universalContext;

    @Inject
    private NodeApi nodeApi;

    @Inject
    private MediaFileRepository mediaFileRepository;

    @Inject
    private MediaFileOwnerRepository mediaFileOwnerRepository;

    @Inject
    private RemoteMediaCacheRepository remoteMediaCacheRepository;

    @Inject
    private MediaOperations mediaOperations;

    @Inject
    private RemoteMediaCacheOperations remoteMediaCacheOperations;

    @Inject
    @PersistenceContext
    private EntityManager entityManager;

    private final ParametrizedLock<String> mediaFileLocks = new ParametrizedLock<>();

    @PostConstruct
    public void init() throws MediaPathNotSetException {
        if (ObjectUtils.isEmpty(config.getMedia().getPath())) {
            throw new MediaPathNotSetException("Path not set");
        }
        try {
            Path path = FileSystems.getDefault().getPath(config.getMedia().getPath());
            if (!Files.exists(path)) {
                throw new MediaPathNotSetException("Not found");
            }
            if (!Files.isDirectory(path)) {
                throw new MediaPathNotSetException("Not a directory");
            }
            if (!Files.isWritable(path)) {
                throw new MediaPathNotSetException("Not writable");
            }
            path = path.resolve(MediaOperations.TMP_DIR);
            if (!Files.exists(path)) {
                try {
                    Files.createDirectory(path);
                } catch (FileAlreadyExistsException e) {
                    // ok
                } catch (Exception e) {
                    throw new MediaPathNotSetException("Cannot create tmp/ subdirectory: " + e.getMessage());
                }
            }
        } catch (InvalidPathException e) {
            throw new MediaPathNotSetException("Path is invalid");
        }
    }

    private TemporaryMediaFile receiveMediaFile(
        String remoteNodeName, String mediaId, ResponseBody responseBody, TemporaryFile tmpFile, int maxSize
    ) throws MoeraNodeException {
        String contentType = Objects.toString(responseBody.contentType(), null);
        if (contentType == null) {
            throw new MoeraNodeException("Response has no Content-Type");
        }
        Long contentLength = responseBody.contentLength() >= 0 ? responseBody.contentLength() : null;
        try {
            DigestingOutputStream out = MediaOperations.transfer(
                responseBody.byteStream(), tmpFile.outputStream(), contentLength, maxSize
            );
            return new TemporaryMediaFile(out.getHash(), contentType, out.getDigest());
        } catch (ThresholdReachedException e) {
            throw new MoeraNodeLocalStorageException(
                "Media %s at %s reports a wrong size or larger than %d bytes"
                    .formatted(mediaId, remoteNodeName, maxSize)
            );
        } catch (IOException e) {
            throw new MoeraNodeLocalStorageException(
                "Error downloading media %s: %s".formatted(mediaId, e.getMessage())
            );
        }
    }

    private TemporaryMediaFile getPublicMedia(
        String nodeName, String id, TemporaryFile tmpFile, int maxSize
    ) throws MoeraNodeException {
        var result = new AtomicReference<TemporaryMediaFile>();
        nodeApi.at(nodeName).getPublicMedia(id, null, null, responseBody ->
            result.set(receiveMediaFile(nodeName, id, responseBody, tmpFile, maxSize))
        );
        return result.get();
    }

    private MediaFile downloadPublicMedia(String nodeName, String id, int maxSize) throws MoeraNodeException {
        if (id == null) {
            return null;
        }

        MediaFile mediaFile = mediaFileRepository.findById(id).orElse(null);
        if (mediaFile != null && mediaFile.isExposed()) {
            return mediaFile;
        }

        try (var ignored = mediaFileLocks.lock(id)) {
            // Could appear in the meantime
            mediaFile = mediaFileRepository.findById(id).orElse(null);
            if (mediaFile != null && mediaFile.isExposed()) {
                return mediaFile;
            }

            var tmp = mediaOperations.tmpFile();
            try {
                var tmpMedia = getPublicMedia(nodeName, id, tmp, maxSize);
                if (!tmpMedia.mediaFileId().equals(id)) {
                    log.warn("Public media {} has hash {}", id, tmpMedia.mediaFileId());
                    return null;
                }
                mediaFile = mediaOperations.putInPlace(id, tmpMedia.contentType(), tmp.path(), null, true);
                // the entity is detached after putInPlace() transaction closed
                mediaFile = entityManager.merge(mediaFile);

                return mediaFile;
            } catch (IOException e) {
                throw new MoeraNodeLocalStorageException(
                    "Error storing public media %s: %s".formatted(id, e.getMessage())
                );
            } finally {
                try {
                    Files.deleteIfExists(tmp.path());
                } catch (IOException e) {
                    log.warn("Error removing temporary media file {}: {}", tmp.path(), e.getMessage());
                }
            }
        }
    }

    public MediaFile downloadPublicMedia(String nodeName, AvatarImage avatarImage) throws MoeraNodeException {
        if (avatarImage == null) {
            return null;
        }
        return downloadPublicMedia(
            nodeName, avatarImage.getMediaId(), universalContext.getOptions().getInt("avatar.max-size")
        );
    }

    public void downloadAvatar(String nodeName, AvatarImage avatarImage) throws MoeraNodeException {
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

    public void downloadAvatars(String nodeName, AvatarImage[] avatarImages) throws MoeraNodeException {
        if (avatarImages != null) {
            for (AvatarImage avatarImage : avatarImages) {
                downloadAvatar(nodeName, avatarImage);
            }
        }
    }

    public void uploadPublicMedia(String nodeName, String carte, MediaFile mediaFile) throws MoeraNodeException {
        if (mediaFile == null) {
            return;
        }
        PublicMediaFileInfo info = nodeApi.at(nodeName).getPublicMediaInfo(mediaFile.getId());
        if (info != null) {
            return;
        }
        nodeApi.at(nodeName, carte).uploadPublicMedia(mediaOperations.getPath(mediaFile), mediaFile.getMimeType());
    }

    public void uploadPublicMedia(String nodeName, String carte, Avatar avatar) throws MoeraNodeException {
        if (avatar == null) {
            return;
        }
        uploadPublicMedia(nodeName, carte, avatar.getMediaFile());
    }

    private TemporaryMediaFile getPrivateMedia(
        String nodeName, String carte, String id, String grant, TemporaryFile tmpFile, int maxSize
    ) throws MoeraNodeException {
        var result = new AtomicReference<TemporaryMediaFile>();
        nodeApi.at(nodeName, carte).getPrivateMedia(
            id, null, null, grant, null,
            responseBody -> result.set(receiveMediaFile(nodeName, id, responseBody, tmpFile, maxSize))
        );
        return result.get();
    }

    private MediaFile getCachedPrivateMedia(
        String nodeName, String carte, String id, String grant, String mediaFileId, String textContent, int maxSize
    ) throws MoeraNodeException, IOException {
        Collection<RemoteMediaCache> caches = remoteMediaCacheRepository.findByMediaWithoutNode(nodeName, id);

        MediaFile mediaFile = caches.stream()
            .map(RemoteMediaCache::getMediaFile)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
        if (mediaFile != null) {
            if (!mediaFile.getId().equals(mediaFileId)) {
                log.warn("Cached media {} has hash {} instead of {}", id, mediaFile.getId(), mediaFileId);
                return null;
            }
            return mediaFile;
        }

        if (caches.stream().anyMatch(remoteMediaCache -> remoteMediaCache.getError() != null)) {
            return null;
        }

        var tmp = mediaOperations.tmpFile();
        try {
            var tmpMedia = getPrivateMedia(nodeName, carte, id, grant, tmp, maxSize);
            if (!tmpMedia.mediaFileId().equals(mediaFileId)) {
                log.warn("Media {} has hash {} instead of {}", id, tmpMedia.mediaFileId(), mediaFileId);
                remoteMediaCacheOperations.error(null, nodeName, id, RemoteMediaError.DIGEST_INCORRECT);
                return null;
            }
            mediaFile = mediaOperations.putInPlace(mediaFileId, tmpMedia.contentType(), tmp.path(), null, false);
            mediaFile.setRecognizedText(textContent);
        } catch (MoeraNodeException e) {
            remoteMediaCacheOperations.error(null, nodeName, id, RemoteMediaError.DOWNLOAD_FAILED);
            throw e;
        } catch (IOException e) {
            remoteMediaCacheOperations.error(null, nodeName, id, RemoteMediaError.STORAGE_ERROR);
            throw e;
        } finally {
            try {
                Files.deleteIfExists(tmp.path());
            } catch (IOException e) {
                log.warn("Error removing temporary media file {}: {}", tmp.path(), e.getMessage());
            }
        }
        remoteMediaCacheOperations.store(null, nodeName, id, mediaFile.getDigest(), mediaFile);

        return mediaFile;
    }

    private void downloadPrivateMediaForCaching(
        String nodeName, String carte, String id, String grant, String mediaFileId, String textContent, int maxSize
    ) throws MoeraNodeException {
        if (id == null) {
            return;
        }

        try (var ignored = mediaFileLocks.lock(mediaFileId)) {
            try {
                getCachedPrivateMedia(nodeName, carte, id, grant, mediaFileId, textContent, maxSize);
            } catch (IOException e) {
                throw new MoeraNodeLocalStorageException(
                    "Error storing private media %s: %s".formatted(id, e.getMessage())
                );
            }
        }
    }

    public void downloadPrivateMediaForCaching(
        String nodeName, String carte, PrivateMediaFileInfo info, int maxSize
    ) throws MoeraNodeException {
        downloadPrivateMediaForCaching(
            nodeName, carte, info.getId(), info.getGrant(), info.getHash(), info.getTextContent(), maxSize
        );
    }

    private MediaFileOwner findAttachedMedia(String mediaFileId, UUID entryId) {
        Collection<MediaFileOwner> mediaFileOwners = mediaFileOwnerRepository
            .findByAdminFile(universalContext.nodeId(), mediaFileId);
        for (MediaFileOwner mediaFileOwner : mediaFileOwners) {
            if (entryId != null) {
                if (mediaOperations.isAttached(mediaFileOwner, entryId)) {
                    return mediaFileOwner;
                }
            } else {
                return mediaFileOwner;
            }
        }

        return null;
    }

    private MediaFileOwner downloadPrivateMedia(
        String nodeName, String carte, String id, String grant, String mediaFileId, String title, String textContent,
        int maxSize, UUID entryId
    ) throws MoeraNodeException {
        if (id == null) {
            return null;
        }

        MediaFileOwner mediaFileOwner = findAttachedMedia(mediaFileId, entryId);
        if (mediaFileOwner != null) {
            return mediaFileOwner;
        }

        // FIXME If long download is needed, other threads waiting for the same media will be locked and will occupy
        // the thread for a long time. However, it will not be a problem with virtual threads. But DB transaction held
        // at the same time is a real problem.
        try (var ignored = mediaFileLocks.lock(mediaFileId)) {
            // Could appear in the meantime
            mediaFileOwner = findAttachedMedia(mediaFileId, entryId);
            if (mediaFileOwner != null) {
                return mediaFileOwner;
            }

            try {
                MediaFile mediaFile = getCachedPrivateMedia(
                    nodeName, carte, id, grant, mediaFileId, textContent, maxSize
                );
                if (mediaFile == null) {
                    return null;
                }
                // Now we are sure that the remote node owns the file with mediaFileId hash, so we can use it
                // for MediaFileOwner

                mediaFile = entityManager.merge(mediaFile); // entity is detached after putInPlace() transaction closed
                mediaFileOwner = mediaOperations.own(mediaFile, null, title);

                return mediaFileOwner;
            } catch (IOException e) {
                throw new MoeraNodeLocalStorageException(
                    "Error storing private media %s: %s".formatted(id, e.getMessage())
                );
            }
        }
    }

    public MediaFileOwner downloadPrivateMedia(
        String nodeName, String carte, PrivateMediaFileInfo info, UUID entryId
    ) throws MoeraNodeException {
        int maxSize = PostingFeaturesUtil.build(universalContext.getOptions(), AccessCheckers.ADMIN).getMediaMaxSize();
        return downloadPrivateMedia(
            nodeName, carte, info.getId(), info.getGrant(), info.getHash(), info.getTitle(), info.getTextContent(),
            maxSize, entryId
        );
    }

    public MediaFileOwner downloadPrivateMediaNoLimits(
        String nodeName, String carte, PrivateMediaFileInfo info
    ) throws MoeraNodeException {
        return downloadPrivateMedia(
            nodeName, carte, info.getId(), info.getGrant(), info.getHash(), info.getTitle(), info.getTextContent(),
            -1, null
        );
    }

    public byte[] getPrivateMediaDigest(String nodeName, String carte, PrivateMediaFileInfo info) {
        return getPrivateMediaDigest(nodeName, carte, info.getId(), info.getGrant(), info.getHash());
    }

    public byte[] getPrivateMediaDigest(String nodeName, String carte, String id, String hash) {
        return getPrivateMediaDigest(nodeName, carte, id, null, hash);
    }

    private byte[] getPrivateMediaDigest(String nodeName, String carte, String id, String grant, String hash) {
        Collection<RemoteMediaCache> caches =
            remoteMediaCacheRepository.findByMedia(universalContext.nodeId(), nodeName, id);
        RemoteMediaCache cache = caches.stream()
            .filter(remoteMediaCache -> remoteMediaCache.getDigest() != null)
            .findFirst()
            .orElse(null);
        if (cache != null) {
            return cache.getDigest();
        }

        if (hash != null) {
            MediaFile mediaFile = mediaFileRepository.findById(hash).orElse(null);
            if (mediaFile != null) {
                remoteMediaCacheOperations.store(null, nodeName, id, mediaFile.getDigest(), mediaFile);
                return mediaFile.getDigest();
            }
        }

        if (caches.stream().anyMatch(remoteMediaCache -> remoteMediaCache.getError() != null)) {
            return null;
        }

        var tmp = mediaOperations.tmpFile();
        try {
            var tmpMedia = getPrivateMedia(
                nodeName, carte, id, grant, tmp, universalContext.getOptions().getInt("media.verification.max-size")
            );
            remoteMediaCacheOperations.store(null, nodeName, id, tmpMedia.digest(), null);
            return tmpMedia.digest();
        } catch (MoeraNodeException e) {
            return null; // TODO need more graceful approach
        } finally {
            try {
                Files.deleteIfExists(tmp.path());
            } catch (IOException e) {
                log.warn("Error removing temporary media file {}: {}", tmp.path(), e.getMessage());
            }
        }
    }

    public void cacheUploadedRemoteMedia(String remoteNodeName, String remoteMediaId, byte[] digest) {
        boolean cached = remoteMediaCacheRepository
            .findByMedia(universalContext.nodeId(), remoteNodeName, remoteMediaId)
            .stream()
            .anyMatch(cache -> cache.getDigest() != null);
        if (!cached) {
            remoteMediaCacheOperations.store(
                universalContext.nodeId(), remoteNodeName, remoteMediaId, digest, null
            );
        }
    }

}
