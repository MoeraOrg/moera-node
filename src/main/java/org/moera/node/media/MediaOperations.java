package org.moera.node.media;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.CREATE;
import static javax.transaction.Transactional.TxType.REQUIRES_NEW;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import javax.inject.Inject;
import javax.transaction.Transactional;

import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.io.input.BoundedInputStream;
import org.moera.commons.crypto.CryptoUtil;
import org.moera.commons.util.LogUtil;
import org.moera.node.config.Config;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.MediaFileOwnerRepository;
import org.moera.node.data.MediaFilePreview;
import org.moera.node.data.MediaFilePreviewRepository;
import org.moera.node.data.MediaFileRepository;
import org.moera.node.global.UniversalContext;
import org.moera.node.model.AvatarDescription;
import org.moera.node.util.DigestingOutputStream;
import org.moera.node.util.Transaction;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.ObjectUtils;

@Component
public class MediaOperations {

    public static final String TMP_DIR = "tmp";

    private static final Logger log = LoggerFactory.getLogger(MediaOperations.class);

    private static final int[] PREVIEW_SIZES = {1400, 900, 150};

    @Inject
    private UniversalContext universalContext;

    @Inject
    private Config config;

    @Inject
    private MediaFileRepository mediaFileRepository;

    @Inject
    private MediaFileOwnerRepository mediaFileOwnerRepository;

    @Inject
    private MediaFilePreviewRepository mediaFilePreviewRepository;

    @Inject
    private PlatformTransactionManager txManager;

    public TemporaryFile tmpFile() {
        while (true) {
            Path path;
            do {
                path = FileSystems.getDefault().getPath(config.getMedia().getPath(), TMP_DIR,
                        CryptoUtil.token().substring(0, 16));
            } while (Files.exists(path));
            try {
                return new TemporaryFile(path, Files.newOutputStream(path, CREATE));
            } catch (IOException e) {
                // next try
            }
        }
    }

    public Path getPath(MediaFile mediaFile) {
        return FileSystems.getDefault().getPath(config.getMedia().getPath(), mediaFile.getFileName());
    }

    public static DigestingOutputStream transfer(InputStream in, OutputStream out, Long contentLength,
                                                 int maxSize) throws IOException {
        DigestingOutputStream digestingStream = new DigestingOutputStream(out);

        out = digestingStream;
        if (contentLength != null) {
            if (contentLength > maxSize) {
                throw new ThresholdReachedException();
            }
            in = new BoundedInputStream(in, contentLength);
        } else {
            out = new BoundedOutputStream(out, maxSize);
        }

        try {
            in.transferTo(out);
        } finally {
            out.close();
        }

        return digestingStream;
    }

    private static Dimension getImageDimension(String contentType, Path path) throws InvalidImageException {
        Iterator<ImageReader> it = ImageIO.getImageReadersByMIMEType(contentType);
        while (it.hasNext()) {
            ImageReader reader = it.next();
            try {
                ImageInputStream stream = new FileImageInputStream(path.toFile());
                reader.setInput(stream);
                int width = reader.getWidth(reader.getMinIndex());
                int height = reader.getHeight(reader.getMinIndex());
                return new Dimension(width, height);
            } catch (IOException e) {
                log.warn("Error reading image file {} (Content-Type: {}): {}",
                        LogUtil.format(path.toString()), LogUtil.format(contentType), e.getMessage());
            } finally {
                reader.dispose();
            }
        }

        throw new InvalidImageException();
    }

    @Transactional(REQUIRES_NEW)
    public MediaFile putInPlace(String id, String contentType, Path tmpPath, byte[] digest) throws IOException {
        MediaFile mediaFile = mediaFileRepository.findById(id).orElse(null);
        if (mediaFile == null) {
            if (digest == null) {
                digest = digest(tmpPath);
            }

            Path mediaPath = FileSystems.getDefault().getPath(
                    config.getMedia().getPath(), MimeUtils.fileName(id, contentType));
            Files.move(tmpPath, mediaPath, REPLACE_EXISTING);

            mediaFile = new MediaFile();
            mediaFile.setId(id);
            mediaFile.setMimeType(contentType);
            if (contentType.startsWith("image/")) {
                mediaFile.setDimension(getImageDimension(contentType, mediaPath));
            }
            mediaFile.setFileSize(Files.size(mediaPath));
            mediaFile.setDigest(digest);
            mediaFile = mediaFileRepository.save(mediaFile);
        }
        return mediaFile;
    }

    public byte[] digest(MediaFile mediaFile) throws IOException {
        return digest(FileSystems.getDefault().getPath(
                config.getMedia().getPath(), MimeUtils.fileName(mediaFile.getId(), mediaFile.getMimeType())));
    }

    private static byte[] digest(Path mediaPath) throws IOException {
        DigestingOutputStream out = new DigestingOutputStream(OutputStream.nullOutputStream());
        try (InputStream in = new FileInputStream(mediaPath.toFile())) {
            in.transferTo(out);
        }
        return out.getDigest();
    }

    private static Rectangle getPreviewRegion(MediaFile mediaFile) {
        int width = mediaFile.getSizeX();
        int height = mediaFile.getSizeY();
        if (mediaFile.getSizeX() > mediaFile.getSizeY() * 5) {
            width = mediaFile.getSizeY() * 5;
        } else if (mediaFile.getSizeY() > mediaFile.getSizeX() * 5) {
            height = mediaFile.getSizeX() * 5;
        }
        return new Rectangle(width, height);
    }

    private MediaFile cropOriginal(MediaFile original) throws IOException {
        var previewFormat = MimeUtils.thumbnail(original.getMimeType());
        if (previewFormat == null) {
            return original;
        }

        Rectangle region = getPreviewRegion(original);
        if (region.getSize().equals(original.getDimension())) {
            return original;
        }

        var tmp = tmpFile();
        try {
            DigestingOutputStream out = new DigestingOutputStream(tmp.getOutputStream());

            Thumbnails.of(getPath(original).toFile())
                    .sourceRegion(region)
                    .size(region.width, region.height)
                    .toOutputStream(out);

            MediaFile cropped = putInPlace(out.getHash(), previewFormat.mimeType, tmp.getPath(), out.getDigest());
            cropped.setExposed(false);
            cropped = mediaFileRepository.save(cropped);

            return cropped;
        } finally {
            Files.deleteIfExists(tmp.getPath());
        }
    }

    private void createPreview(MediaFile original, MediaFile cropped, int width) throws IOException {
        var previewFormat = MimeUtils.thumbnail(original.getMimeType());
        if (previewFormat == null) {
            return;
        }

        MediaFilePreview largerPreview = original.findLargerPreview(width);
        if (largerPreview != null && largerPreview.getWidth() == width) {
            return;
        }

        MediaFile previewFile = cropped;
        if (cropped.getSizeX() > width) {
            var tmp = tmpFile();
            try {
                DigestingOutputStream out = new DigestingOutputStream(tmp.getOutputStream());

                Thumbnails.of(getPath(cropped).toFile())
                        .width(width)
                        .outputFormat(previewFormat.format)
                        .toOutputStream(out);

                long fileSize = Files.size(tmp.getPath());
                long prevFileSize = largerPreview != null
                        ? largerPreview.getMediaFile().getFileSize()
                        : cropped.getFileSize();
                long gain = (prevFileSize - fileSize) * 100 / prevFileSize; // negative, if fileSize > prevFileSize
                if (gain < universalContext.getOptions().getInt("media.preview-gain")) {
                    if (largerPreview != null) {
                        return;
                    }
                    // otherwise original will be used in preview
                } else {
                    previewFile = putInPlace(out.getHash(), previewFormat.mimeType, tmp.getPath(), out.getDigest());
                    previewFile.setExposed(false);
                    previewFile = mediaFileRepository.save(previewFile);
                }
            } finally {
                Files.deleteIfExists(tmp.getPath());
            }
        }

        MediaFilePreview preview = new MediaFilePreview();
        preview.setId(UUID.randomUUID());
        preview.setOriginalMediaFile(original);
        preview.setWidth(width);
        preview.setMediaFile(previewFile);
        preview = mediaFilePreviewRepository.save(preview);
        original.addPreview(preview);
    }

    public MediaFileOwner own(MediaFile mediaFile, String ownerName) throws IOException {
        MediaFile croppedFile = cropOriginal(mediaFile);
        for (int size : PREVIEW_SIZES) {
            createPreview(mediaFile, croppedFile, size);
        }

        MediaFileOwner mediaFileOwner = new MediaFileOwner();
        mediaFileOwner.setId(UUID.randomUUID());
        mediaFileOwner.setNodeId(universalContext.nodeId());
        mediaFileOwner.setOwnerName(ownerName);
        mediaFileOwner.setMediaFile(mediaFile);

        return mediaFileOwnerRepository.save(mediaFileOwner);
    }

    public ResponseEntity<Resource> serve(MediaFile mediaFile) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf(mediaFile.getMimeType()));

        switch (config.getMedia().getServe().toLowerCase()) {
            default:
            case "stream": {
                headers.setContentLength(mediaFile.getFileSize());
                Path mediaPath = getPath(mediaFile);
                return new ResponseEntity<>(new FileSystemResource(mediaPath), headers, HttpStatus.OK);
            }

            case "accel":
                headers.add("X-Accel-Redirect", config.getMedia().getAccelPrefix() + mediaFile.getFileName());
                return new ResponseEntity<>(headers, HttpStatus.OK);

            case "sendfile": {
                Path mediaPath = getPath(mediaFile);
                headers.add("X-SendFile", mediaPath.toAbsolutePath().toString());
                return new ResponseEntity<>(headers, HttpStatus.OK);
            }
        }
    }

    public ResponseEntity<Resource> serve(MediaFile mediaFile, Integer width) {
        if (width == null) {
            return serve(mediaFile);
        }

        MediaFilePreview preview = mediaFile.findLargerPreview(width);
        return serve(preview != null ? preview.getMediaFile() : mediaFile);
    }

    public void validateAvatar(AvatarDescription avatar, Consumer<MediaFile> found,
                               Supplier<RuntimeException> notFound) {
        if (avatar == null || avatar.getMediaId() == null) {
            return;
        }

        MediaFile mediaFile = mediaFileRepository.findById(avatar.getMediaId()).orElse(null);
        if (mediaFile != null && !mediaFile.isExposed()) {
            mediaFile = null;
        }
        if (mediaFile == null && (avatar.getOptional() == null || !avatar.getOptional())) {
            throw notFound.get();
        } else {
            found.accept(mediaFile);
        }
    }

    public List<MediaFileOwner> validateAttachments(UUID[] ids, Supplier<RuntimeException> notFound,
                                                    boolean isAdmin, String clientName) {
        if (ObjectUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }

        List<MediaFileOwner> attached = new ArrayList<>();
        Map<UUID, MediaFileOwner> mediaFileOwners = mediaFileOwnerRepository.findByIds(universalContext.nodeId(), ids)
                .stream().collect(Collectors.toMap(MediaFileOwner::getId, Function.identity()));
        for (UUID id : ids) {
            MediaFileOwner mediaFileOwner = mediaFileOwners.get(id);
            if (mediaFileOwner == null) {
                throw notFound.get();
            }
            if (mediaFileOwner.getOwnerName() == null && !isAdmin
                    || mediaFileOwner.getOwnerName() != null
                        && !mediaFileOwner.getOwnerName().equals(clientName)) {
                throw notFound.get();
            }
            attached.add(mediaFileOwner);
        }
        return attached;
    }

    @Scheduled(fixedDelayString = "PT6H")
    public void purgeUnused() throws Throwable {
        Timestamp now = Util.now();
        Transaction.execute(txManager, () -> {
            mediaFileOwnerRepository.deleteUnused(now);
            return null;
        });
        List<Path> fileNames = mediaFileRepository.findUnused(now).stream()
                .map(this::getPath)
                .collect(Collectors.toList());
        Transaction.execute(txManager, () -> {
            mediaFileRepository.deleteUnused(now);
            return null;
        });
        for (Path path : fileNames) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                log.warn("Error deleting {}: {}", path, e.getMessage());
                // ignore
            }
        }
    }

}
