package org.moera.node.media;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.CREATE;
import static javax.transaction.Transactional.TxType.REQUIRES_NEW;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

import com.drew.imaging.FileType;
import com.drew.imaging.FileTypeDetector;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
import org.apache.commons.io.input.BoundedInputStream;
import org.moera.commons.crypto.CryptoUtil;
import org.moera.commons.util.LogUtil;
import org.moera.node.auth.principal.AccessCheckers;
import org.moera.node.auth.principal.Principal;
import org.moera.node.config.Config;
import org.moera.node.data.Comment;
import org.moera.node.data.Entry;
import org.moera.node.data.EntryAttachmentRepository;
import org.moera.node.data.EntryRepository;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.MediaFileOwnerRepository;
import org.moera.node.data.MediaFilePreview;
import org.moera.node.data.MediaFilePreviewRepository;
import org.moera.node.data.MediaFileRepository;
import org.moera.node.data.Posting;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.global.UniversalContext;
import org.moera.node.model.AvatarDescription;
import org.moera.node.model.PostingFeatures;
import org.moera.node.task.Jobs;
import org.moera.node.task.JobsManagerInitializedEvent;
import org.moera.node.util.DigestingOutputStream;
import org.moera.node.util.Transaction;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class MediaOperations {

    public static final String TMP_DIR = "tmp";
    public static final String PUBLIC_DIR = "public";

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
    private EntryAttachmentRepository entryAttachmentRepository;

    @Inject
    private EntryRepository entryRepository;

    @Inject
    private StoryRepository storyRepository;

    @Inject
    private Transaction tx;

    @Inject
    private Jobs jobs;

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

    public Path getPublicServingPath() {
        return FileSystems.getDefault().getPath(config.getMedia().getPath(), PUBLIC_DIR);
    }

    public Path getPublicServingPath(MediaFile mediaFile) {
        return getPublicServingPath().resolve(mediaFile.getFileName());
    }

    public void createPublicServingLink(MediaFile mediaFile) throws IOException {
        Path publicPath = getPublicServingPath(mediaFile);
        if (!Files.exists(publicPath, LinkOption.NOFOLLOW_LINKS)) {
            Files.createSymbolicLink(publicPath, Paths.get("..", mediaFile.getFileName()));
        }
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
    public MediaFile putInPlace(String id, String contentType, Path tmpPath, byte[] digest,
                                boolean exposed) throws IOException {
        MediaFile mediaFile = mediaFileRepository.findById(id).orElse(null);
        if (mediaFile == null) {
            if (digest == null) {
                digest = digest(tmpPath);
            }
            if (contentType == null || contentType.startsWith("image/")) {
                contentType = detectContentType(tmpPath, contentType);
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
            mediaFile.setOrientation(getImageOrientation(mediaPath));
            mediaFile.setFileSize(Files.size(mediaPath));
            mediaFile.setDigest(digest);
            mediaFile.setExposed(exposed);
            mediaFile = mediaFileRepository.save(mediaFile);

            if (config.getMedia().isDirectServe() && exposed) {
                createPublicServingLink(mediaFile);
            }
        }
        return mediaFile;
    }

    private short getImageOrientation(Path imagePath) {
        short orientation = 1;
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(imagePath.toFile());
            if (metadata != null) {
                Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
                if (directory != null) {
                    orientation = (short) directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
                }
            }
        } catch (MetadataException | IOException | ImageProcessingException e) {
            // Could not get orientation, use default
        }
        return orientation;
    }

    private String detectContentType(Path path, String defaultContentType) throws IOException {
        FileType fileType = FileTypeDetector.detectFileType(new BufferedInputStream(new FileInputStream(path.toFile())));
        return fileType != null ? fileType.getMimeType() : defaultContentType;
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

            ThumbnailUtil.thumbnailOf(getPath(original).toFile(), original.getMimeType())
                    .sourceRegion(region)
                    .size(region.width, region.height)
                    .toOutputStream(out);

            MediaFile cropped = putInPlace(
                    out.getHash(), previewFormat.mimeType, tmp.getPath(), out.getDigest(), false);
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

                ThumbnailUtil.thumbnailOf(getPath(cropped).toFile(), cropped.getMimeType())
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
                    previewFile = putInPlace(
                            out.getHash(), previewFormat.mimeType, tmp.getPath(), out.getDigest(), false);
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

    private Principal entryViewPrincipal(Entry entry) {
        Principal view = entry.getViewCompound();
        if (entry.getParent() != null) {
            view = view.intersect(
                    entry.getParent().getViewCompound().intersect(entry.getParent().getViewCommentsCompound()));
        }
        return view;
    }

    public void updatePermissions(MediaFileOwner mediaFileOwner) {
        if (mediaFileOwner == null) {
            return;
        }

        Collection<Entry> entries =
                entryAttachmentRepository.findByMedia(mediaFileOwner.getNodeId(), mediaFileOwner.getId());
        Principal view = entries.stream()
                .map(this::entryViewPrincipal)
                .reduce(Principal.NONE, Principal::union);
        mediaFileOwner.setViewPrincipal(view);
        mediaFileOwner.setPermissionsUpdatedAt(Util.now());
        for (Posting posting : mediaFileOwner.getPostings()) {
            List<Entry> list = entries.stream()
                    .filter(e -> Objects.equals(e.getReceiverName(), posting.getReceiverName()))
                    .toList();
            list.forEach(e -> posting.setAcceptedReactionsPositive(e.getAcceptedReactionsPositive()));
            list.forEach(e -> posting.setAcceptedReactionsNegative(e.getAcceptedReactionsNegative()));
            Principal principal = list.stream()
                    .map(this::entryViewPrincipal)
                    .reduce(Principal.NONE, Principal::union);
            posting.setViewPrincipal(principal);
            principal = list.stream()
                    .map(Entry::getViewCommentsPrincipal)
                    .reduce(Principal.NONE, Principal::union);
            posting.setViewCommentsPrincipal(principal);
            principal = list.stream()
                    .map(Entry::getAddCommentPrincipal)
                    .reduce(Principal.NONE, Principal::union);
            posting.setAddCommentPrincipal(principal);
            principal = list.stream()
                    .map(Entry::getViewReactionsPrincipal)
                    .reduce(Principal.NONE, Principal::union);
            posting.setViewReactionsPrincipal(principal);
            principal = list.stream()
                    .map(Entry::getViewNegativeReactionsPrincipal)
                    .reduce(Principal.NONE, Principal::union);
            posting.setViewNegativeReactionsPrincipal(principal);
            principal = list.stream()
                    .map(Entry::getViewReactionTotalsPrincipal)
                    .reduce(Principal.NONE, Principal::union);
            posting.setViewReactionTotalsPrincipal(principal);
            principal = list.stream()
                    .map(Entry::getViewNegativeReactionTotalsPrincipal)
                    .reduce(Principal.NONE, Principal::union);
            posting.setViewNegativeReactionTotalsPrincipal(principal);
            principal = list.stream()
                    .map(Entry::getViewReactionRatiosPrincipal)
                    .reduce(Principal.NONE, Principal::union);
            posting.setViewReactionRatiosPrincipal(principal);
            principal = list.stream()
                    .map(Entry::getViewNegativeReactionRatiosPrincipal)
                    .reduce(Principal.NONE, Principal::union);
            posting.setViewNegativeReactionTotalsPrincipal(principal);
            principal = list.stream()
                    .map(Entry::getAddReactionPrincipal)
                    .reduce(Principal.NONE, Principal::union);
            posting.setAddReactionPrincipal(principal);
            principal = list.stream()
                    .map(Entry::getAddNegativeReactionPrincipal)
                    .reduce(Principal.NONE, Principal::union);
            posting.setAddNegativeReactionPrincipal(principal);
        }
    }

    public void updatePermissions(Entry entry) {
        if (entry.getCurrentRevision() != null) {
            entry.getCurrentRevision().getAttachments().forEach(ea -> updatePermissions(ea.getMediaFileOwner()));
        }
    }

    public ResponseEntity<Resource> serve(MediaFile mediaFile, boolean download) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf(mediaFile.getMimeType()));
        if (download) {
            headers.setContentDisposition(ContentDisposition.attachment().build());
        }
        headers.setAccessControlAllowOrigin("*");

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

    public ResponseEntity<Resource> serve(MediaFile mediaFile, Integer width, Boolean download) {
        download = download != null ? download : false;
        if (width == null) {
            return serve(mediaFile, download);
        }

        MediaFilePreview preview = mediaFile.findLargerPreview(width);
        return serve(preview != null ? preview.getMediaFile() : mediaFile, download);
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
                                                    Supplier<RuntimeException> notCompressed,
                                                    boolean isAdmin, String clientName) {
        if (ObjectUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }

        PostingFeatures features = new PostingFeatures(universalContext.getOptions(), AccessCheckers.ADMIN);
        int recommendedSize = features.getImageRecommendedSize();

        List<MediaFileOwner> attached = new ArrayList<>();
        Set<UUID> usedIds = new HashSet<>();
        Map<UUID, MediaFileOwner> mediaFileOwners = mediaFileOwnerRepository.findByIds(universalContext.nodeId(), ids)
                .stream().collect(Collectors.toMap(MediaFileOwner::getId, Function.identity()));
        for (UUID id : ids) {
            if (usedIds.contains(id)) {
                continue;
            }
            MediaFileOwner mediaFileOwner = mediaFileOwners.get(id);
            if (mediaFileOwner == null) {
                throw notFound.get();
            }
            if (mediaFileOwner.getOwnerName() == null && !isAdmin
                    || mediaFileOwner.getOwnerName() != null
                        && !mediaFileOwner.getOwnerName().equals(clientName)) {
                throw notFound.get();
            }
            if (notCompressed != null && !isAdmin && mediaFileOwner.getMediaFile().getFileSize() > recommendedSize) {
                throw notCompressed.get();
            }
            attached.add(mediaFileOwner);
            usedIds.add(id);
        }
        return attached;
    }

    public List<Story> getParentStories(UUID mediaFileOwnerId) {
        Set<Entry> entries = entryRepository.findByMediaId(mediaFileOwnerId);
        return entries.stream()
                .map(entry -> entry instanceof Comment ? entry.getParent().getId() : entry.getId())
                .map(id -> storyRepository.findByEntryId(universalContext.nodeId(), id))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    @Scheduled(fixedDelayString = "PT6H")
    public void purgeUnused() {
        Timestamp now = Util.now();
        tx.executeWrite(() -> mediaFileOwnerRepository.deleteUnused(now));
        Collection<MediaFile> mediaFiles = tx.executeRead(() -> mediaFileRepository.findUnused(now));
        tx.executeWrite(() -> mediaFileRepository.deleteUnused(now));
        for (MediaFile mediaFile : mediaFiles) {
            Path path = getPath(mediaFile);
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                log.warn("Error deleting {}: {}", path, e.getMessage());
            }
            if (config.getMedia().isDirectServe() && mediaFile.isExposed()) {
                Path publicPath = getPublicServingPath(mediaFile);
                try {
                    Files.deleteIfExists(publicPath);
                } catch (IOException e) {
                    log.warn("Error deleting {}: {}", publicPath, e.getMessage());
                }
            }
        }
    }

    @Scheduled(fixedDelayString = "PT15M")
    @Transactional
    public void refreshPermissions() {
        mediaFileOwnerRepository.findOutdatedPermissions().forEach(this::updatePermissions);
    }

    @EventListener(JobsManagerInitializedEvent.class)
    public void prepareDirectServing() throws IOException {
        if (!config.getMedia().isDirectServe()) {
            return;
        }
        Path publicDir = getPublicServingPath();
        if (!Files.exists(publicDir)) {
            Files.createDirectory(publicDir);
            jobs.run(PrepareDirectServingJob.class, new PrepareDirectServingJob.Parameters());
        }
    }

}
