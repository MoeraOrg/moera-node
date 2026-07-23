package org.moera.node.media;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.moera.lib.http.Response;
import org.moera.lib.node.exception.MoeraNodeException;
import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.node.types.MediaAttachment;
import org.moera.lib.node.types.MediaToAttach;
import org.moera.lib.node.types.PostingInfo;
import org.moera.lib.node.types.PrivateMediaFileInfo;
import org.moera.lib.node.types.PublicMediaFileInfo;
import org.moera.node.api.node.MoeraNodeLocalStorageException;
import org.moera.node.api.node.NodeApi;
import org.moera.node.config.Config;
import org.moera.node.data.Avatar;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.MediaFileOwnerRepository;
import org.moera.node.data.MediaFileRepository;
import org.moera.node.data.MediaUpload;
import org.moera.node.data.MediaUploadRepository;
import org.moera.node.data.RemoteMediaCache;
import org.moera.node.data.RemoteMediaCacheRepository;
import org.moera.node.data.RemoteMediaError;
import org.moera.node.global.UniversalContext;
import org.moera.node.model.AvatarImageUtil;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.OperationFailure;
import org.moera.node.ocrspace.OcrSpace;
import org.moera.node.util.DigestingOutputStream;
import org.moera.node.util.ParametrizedLock;
import org.moera.node.util.UriUtil;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

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
    private MediaUploadRepository mediaUploadRepository;

    @Inject
    private MediaOperations mediaOperations;

    @Inject
    private MediaUploadOperations mediaUploadOperations;

    @Inject
    private RemoteMediaCacheOperations remoteMediaCacheOperations;

    @Inject
    private MediaDownloadOperations mediaDownloadOperations;

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
            initMediaSubdirectory(MediaOperations.TMP_DIR);
            initMediaSubdirectory(MediaUploadOperations.UPLOADS_DIR);
        } catch (InvalidPathException e) {
            throw new MediaPathNotSetException("Path is invalid");
        }
    }

    private void initMediaSubdirectory(String dirName) throws MediaPathNotSetException {
        Path path = FileSystems.getDefault().getPath(config.getMedia().getPath(), dirName);
        if (!Files.exists(path)) {
            try {
                Files.createDirectory(path);
            } catch (FileAlreadyExistsException e) {
                // ok
            } catch (Exception e) {
                throw new MediaPathNotSetException(
                    String.format("Cannot create %s/ subdirectory: %s", dirName, e.getMessage())
                );
            }
        }
    }

    private TemporaryMediaFile receiveMediaFile(
        String remoteNodeName, String mediaId, Response responseBody, TemporaryFile tmpFile, int maxSize
    ) throws MoeraNodeException {
        if (responseBody.contentType() == null) {
            throw new MoeraNodeException("Response has no Content-Type");
        }
        Long contentLength = responseBody.contentLength() >= 0 ? responseBody.contentLength() : null;
        try {
            DigestingOutputStream out = MediaOperations.transfer(
                responseBody.bodyStream(), tmpFile.outputStream(), contentLength, maxSize
            );
            return new TemporaryMediaFile(out.getHash(), responseBody.contentType(), out.getDigest());
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
        try {
            nodeApi.at(nodeName, carte).uploadPublicMedia(
                mediaOperations.getPath(mediaFile), mediaFile.getMimeType()
            );
        } catch (MediaFileNotAvailableException e) {
            throw new MoeraNodeLocalStorageException(e);
        }
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
        if (!Objects.equals(nodeName, universalContext.nodeName()) && grant != null) {
            carte = null; // use the grant for authentication
        }
        var result = new AtomicReference<TemporaryMediaFile>();
        nodeApi.at(nodeName, carte).getPrivateMedia(
            id, null, null, grant, null,
            responseBody -> result.set(receiveMediaFile(nodeName, id, responseBody, tmpFile, maxSize))
        );
        return result.get();
    }

    private MediaFile getCachedPrivateMedia(
        String nodeName,
        String carte,
        String id,
        String grant,
        String mediaFileId,
        String textContent,
        int maxSize
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

    public void downloadPrivateMediaForCaching(
        String nodeName,
        String carte,
        PrivateMediaFileInfo info,
        int maxSize
    ) throws MoeraNodeException {
        if (info == null || info.getId() == null) {
            return;
        }

        try (var ignored = mediaFileLocks.lock(info.getHash())) {
            try {
                getCachedPrivateMedia(
                    nodeName, carte, info.getId(), info.getGrant(), info.getHash(), info.getTextContent(), maxSize
                );
            } catch (IOException e) {
                throw new MoeraNodeLocalStorageException(
                    "Error storing private media %s: %s".formatted(info.getId(), e.getMessage())
                );
            }
        }
    }

    private MediaFileOwner findAttachedMedia(String mediaFileId, UUID entryId) {
        Collection<MediaFileOwner> mediaFileOwners = mediaFileOwnerRepository
            .findByFile(universalContext.nodeId(), mediaFileId);
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
        String nodeName,
        String carte,
        String id,
        String grant,
        String mediaFileId,
        String title,
        String textContent,
        int maxSize,
        UUID entryId
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
                mediaFileOwner = mediaOperations.own(mediaFile, title);

                return mediaFileOwner;
            } catch (IOException e) {
                throw new MoeraNodeLocalStorageException(
                    "Error storing private media %s: %s".formatted(id, e.getMessage())
                );
            }
        }
    }

    public MediaFileOwner downloadPrivateMedia(
        String nodeName,
        String carte,
        PrivateMediaFileInfo info,
        int maxSize,
        UUID entryId
    ) throws MoeraNodeException {
        return downloadPrivateMedia(
            nodeName,
            carte,
            info.getId(),
            info.getGrant(),
            info.getHash(),
            info.getTitle(),
            info.getTextContent(),
            maxSize,
            entryId
        );
    }

    public MediaFileOwner downloadPrivateMediaNoLimits(
        String nodeName,
        String carte,
        PrivateMediaFileInfo info
    ) throws MoeraNodeException {
        return downloadPrivateMedia(
            nodeName,
            carte,
            info.getId(),
            info.getGrant(),
            info.getHash(),
            info.getTitle(),
            info.getTextContent(),
            -1,
            null
        );
    }

    private record CachedDigest(byte[] digest, boolean error) {
    }

    private CachedDigest getCachedPrivateMediaDigest(String nodeName, String mediaId, String hash) {
        if (Objects.equals(nodeName, universalContext.nodeName())) {
            MediaFileOwner media = mediaFileOwnerRepository.findById(UUID.fromString(mediaId)).orElse(null);
            if (media != null) {
                return new CachedDigest(media.getMediaFile().getDigest(), false);
            } else {
                return new CachedDigest(null, true);
            }
        }

        Collection<RemoteMediaCache> caches =
            remoteMediaCacheRepository.findByMedia(universalContext.nodeId(), nodeName, mediaId);
        RemoteMediaCache cache = caches.stream()
            .filter(remoteMediaCache -> remoteMediaCache.getDigest() != null)
            .findFirst()
            .orElse(null);
        if (cache != null && cache.getDigest() != null) {
            return new CachedDigest(cache.getDigest(), false);
        }
        boolean error = caches.stream().anyMatch(remoteMediaCache -> remoteMediaCache.getError() != null);

        if (hash != null) {
            MediaFile mediaFile = mediaFileRepository.findById(hash).orElse(null);
            if (mediaFile != null) {
                remoteMediaCacheOperations.store(null, nodeName, mediaId, mediaFile.getDigest(), mediaFile);
                return new CachedDigest(mediaFile.getDigest(), false);
            }
        }

        return new CachedDigest(null, error);
    }

    private byte[] getPrivateMediaDigest(String nodeName, String carte, PrivateMediaFileInfo info) {
        var cached = getCachedPrivateMediaDigest(nodeName, info.getId(), info.getHash());
        if (cached.digest() != null) {
            return cached.digest();
        }
        if (cached.error()) {
            return null;
        }

        var tmp = mediaOperations.tmpFile();
        try {
            Integer maxSize = universalContext.getOptions().getInt("media.verification.max-size");
            if (info.getSize() > maxSize) {
                return Util.base64decode(info.getDigest());
            }
            var tmpMedia = getPrivateMedia(nodeName, carte, info.getId(), info.getGrant(), tmp, maxSize);
            remoteMediaCacheOperations.store(null, nodeName, info.getId(), tmpMedia.digest(), null);
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

    public byte[] getPrivateMediaDigest(
        String nodeName,
        Function<String, String> carteGenerator,
        MediaAttachment attachment
    ) {
        PrivateMediaFileInfo mediaInfo;
        if (attachment.getMedia() == null) {
            var remoteMedia = attachment.getRemoteMedia();
            if (remoteMedia == null) {
                return null;
            }
            nodeName = remoteMedia.getNodeName();
            var cached = getCachedPrivateMediaDigest(nodeName, remoteMedia.getMediaId(), remoteMedia.getHash());
            if (cached.digest() != null) {
                return cached.digest();
            }
            if (cached.error()) {
                return null;
            }
            try {
                mediaInfo = nodeApi.at(remoteMedia.getNodeName(), carteGenerator.apply(nodeName))
                    .getPrivateMediaInfo(remoteMedia.getMediaId(), remoteMedia.getGrant());
            } catch (MoeraNodeException e) {
                return null; // TODO need more graceful approach
            }
        } else {
            mediaInfo = attachment.getMedia();
        }
        if (mediaInfo == null) {
            return null;
        }

        return getPrivateMediaDigest(nodeName, carteGenerator.apply(nodeName), mediaInfo);
    }

    /**
     * For quick calculations in user-faced API endpoints and for attachments generated by ourselves
     */
    public byte[] getTrustedPrivateMediaDigest(MediaToAttach attachment) {
        if (attachment.getLocalMediaId() != null) {
            var cached = getCachedPrivateMediaDigest(universalContext.nodeName(), attachment.getLocalMediaId(), null);
            return cached.digest();
        }

        var remoteMedia = attachment.getRemoteMedia();
        if (remoteMedia == null) {
            return null;
        }
        String nodeName = remoteMedia.getNodeName();
        var cached = getCachedPrivateMediaDigest(nodeName, remoteMedia.getMediaId(), remoteMedia.getHash());
        if (cached.digest() != null) {
            return cached.digest();
        }
        if (cached.error()) {
            return null;
        }
        return Util.base64decode(remoteMedia.getDigest());
    }

    public byte[] getParentMediaDigest(
        PostingInfo postingInfo,
        String defaultNodeName,
        Function<String, String> carteGenerator
    ) throws MoeraNodeException {
        var parentMedia = postingInfo.getParentMedia();
        if (parentMedia == null || parentMedia.getMediaId() == null) {
            return null;
        }

        String parentMediaNodeName = parentMedia.getNodeName() != null ? parentMedia.getNodeName() : defaultNodeName;
        var cached = getCachedPrivateMediaDigest(parentMediaNodeName, parentMedia.getMediaId(), null);
        if (cached.digest() != null) {
            return cached.digest();
        }
        if (cached.error()) {
            return null;
        }

        if (parentMedia.getPostingId() == null) {
            return null;
        }
        List<MediaAttachment> attachments;
        var carte = carteGenerator.apply(parentMediaNodeName);
        if (parentMedia.getCommentId() == null) {
            var posting = nodeApi.at(parentMediaNodeName, carte).getPosting(parentMedia.getPostingId(), false);
            attachments = posting != null ? posting.getMedia() : null;
        } else {
            var comment = nodeApi.at(parentMediaNodeName, carte)
                .getComment(parentMedia.getPostingId(), parentMedia.getCommentId(), false);
            attachments = comment != null ? comment.getMedia() : null;
        }
        if (ObjectUtils.isEmpty(attachments)) {
            return null;
        }
        var attachment = attachments.stream().filter(ma -> ma.getPostingId().equals(postingInfo.getId()))
            .findFirst().orElse(null);
        if (attachment == null) {
            return null;
        }

        return getPrivateMediaDigest(parentMediaNodeName, carteGenerator, attachment);
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

    public MediaFileOwner ownMedia(
        MediaType mediaType,
        Long contentLength,
        String contentDisposition,
        String upload,
        String url,
        boolean downsize,
        InputStream in
    ) throws IOException {
        MediaFileOwner mediaFileOwner;
        if (!ObjectUtils.isEmpty(url)) {
            mediaFileOwner = ownMediaFromUrl(url, downsize);
        } else if (!ObjectUtils.isEmpty(upload)) {
            mediaFileOwner = ownMediaFromUpload(upload, downsize);
        } else {
            mediaFileOwner = ownMediaFromStream(in, mediaType, contentLength, contentDisposition, downsize);
        }

        if (isSuitableForOcr(mediaFileOwner.getMediaFile())) {
            mediaFileOwner.getMediaFile().setRecognizeAt(Util.now());
        }

        return mediaFileOwner;
    }

    private MediaFileOwner ownMediaFromStream(
        InputStream in,
        MediaType mediaType,
        Long contentLength,
        String fileName,
        boolean downsize
    ) throws IOException {
        var tmp = mediaOperations.tmpFile();
        try {
            DigestingOutputStream out = transfer(in, tmp.outputStream(), contentLength, null);
            String contentType = toContentType(mediaType);
            if (downsize) {
                contentType = mediaOperations.downsizeImage(tmp.path(), contentType);
                try (InputStream tmpIn = new FileInputStream(tmp.path().toFile())) {
                    out = transfer(tmpIn, null, null, null);
                }
            }

            MediaFile mediaFile = mediaOperations.putInPlace(
                out.getHash(), contentType, tmp.path(), out.getDigest(), false
            );
            // the entity is detached after putInPlace() transaction closed
            mediaFile = entityManager.merge(mediaFile);
            String title = uploadedFileName(contentType, fileName);
            return mediaOperations.own(mediaFile, title);
        } finally {
            Files.deleteIfExists(tmp.path());
        }
    }

    private MediaFileOwner ownMediaFromUpload(String id, boolean downsize) throws IOException {
        UUID uploadId = Util.uuid(id).orElseThrow(() -> new ObjectNotFoundFailure("media-upload.not-found"));
        try (var ignored = mediaUploadOperations.lock(uploadId)) {
            MediaUpload mediaUpload = mediaUploadRepository.findByNodeIdAndId(universalContext.nodeId(), uploadId)
                .orElseThrow(() -> new ObjectNotFoundFailure("media-upload.not-found"));
            if (!mediaUpload.isCompleted()) {
                throw new OperationFailure("media-upload.not-completed");
            }
            Path path = mediaUploadOperations.getPath(mediaUpload);
            String contentType = mediaUpload.getMimeType();
            if (downsize) {
                contentType = mediaOperations.downsizeImage(path, contentType);
            }
            DigestingOutputStream out;
            try (InputStream tmpIn = new FileInputStream(path.toFile())) {
                out = transfer(tmpIn, null, null, null);
            }

            MediaFile mediaFile = mediaOperations.putInPlace(
                out.getHash(), contentType, path, out.getDigest(), false
            );
            // the entity is detached after putInPlace() transaction closed
            mediaFile = entityManager.merge(mediaFile);
            String fileName = uploadedFileName(contentType, mediaUpload.getTitle());
            MediaFileOwner mediaFileOwner = mediaOperations.own(mediaFile, fileName);
            // if the file already exists, the uploaded one will not be moved out
            mediaUploadOperations.deleteUploadFileQuietly(mediaUpload);
            mediaUploadRepository.deleteByNodeIdAndId(universalContext.nodeId(), uploadId);

            return mediaFileOwner;
        }
    }

    private MediaFileOwner ownMediaFromUrl(String url, boolean downsize) throws IOException {
        var mediaStream = mediaDownloadOperations.fetchMedia(url);

        var tmp = mediaOperations.tmpFile();
        try {
            DigestingOutputStream out = transfer(
                mediaStream.stream(), tmp.outputStream(), mediaStream.contentLength(), null
            );
            String contentType = toContentType(mediaStream.contentType());
            if (downsize) {
                contentType = mediaOperations.downsizeImage(tmp.path(), contentType);
                try (InputStream tmpIn = new FileInputStream(tmp.path().toFile())) {
                    out = transfer(tmpIn, null, null, null);
                }
            }

            MediaFile mediaFile = mediaOperations.putInPlace(
                out.getHash(), contentType, tmp.path(), out.getDigest(), false
            );
            // the entity is detached after putInPlace() transaction closed
            mediaFile = entityManager.merge(mediaFile);
            String fileName = uploadedFileName(contentType, UriUtil.fileName(url));
            return mediaOperations.own(mediaFile, fileName);
        } finally {
            Files.deleteIfExists(tmp.path());
        }
    }

    private DigestingOutputStream transfer(
        InputStream in, OutputStream out, Long contentLength, Integer maxSize
    ) throws IOException {
        if (maxSize == null) {
            maxSize = universalContext.getOptions().getInt("media.max-size");
        }
        return MediaOperations.transfer(in, out, contentLength, maxSize);
    }

    private String toContentType(MediaType mediaType) {
        return mediaType != null ? mediaType.getType() + "/" + mediaType.getSubtype() : null;
    }

    private String uploadedFileName(String contentType, String fileName) {
        return !MimeUtil.isSupportedImage(contentType) && !ObjectUtils.isEmpty(fileName)
            ? StringUtils.stripFilenameExtension(StringUtils.getFilename(fileName))
            : null;
    }

    private boolean isSuitableForOcr(MediaFile mediaFile) {
        return (mediaFile.isImage() || MediaType.APPLICATION_PDF_VALUE.equals(mediaFile.getMimeType()))
            && mediaFile.getFileSize() < OcrSpace.MAX_FILE_SIZE;
    }

}
